#!/usr/bin/env groovy
/**
 * collectBuildMetrics - Collect build metrics for ML training
 * 
 * Usage:
 *   collectBuildMetrics.start(buildId: env.BUILD_TAG)
 *   // ... your build steps ...
 *   collectBuildMetrics.stop(buildId: env.BUILD_TAG, status: 'SUCCESS')
 */

import org.ml.nodeselection.GitAnalyzer
import org.ml.nodeselection.MetricsLogger

def start(Map config = [:]) {
    def buildId = config.buildId ?: env.BUILD_TAG
    def metricsPath = config.metricsPath ?: "${env.JENKINS_HOME}/ml-metrics"
    
    echo "📈 Starting metrics collection for ${buildId}"
    
    // Store start time
    env.ML_BUILD_START = System.currentTimeMillis().toString()
    
    // Collect git metrics
    def gitAnalyzer = new GitAnalyzer(this)
    def gitMetrics = gitAnalyzer.analyze()
    
    // Store in environment for later
    env.ML_FILES_CHANGED = gitMetrics.filesChanged.toString()
    env.ML_LINES_ADDED = gitMetrics.linesAdded.toString()
    env.ML_LINES_DELETED = gitMetrics.linesDeleted.toString()
    env.ML_BRANCH = gitMetrics.branch
    env.ML_DEPS_CHANGED = gitMetrics.depsChanged.toString()
    
    // Start background resource monitoring
    if (isUnix()) {
        sh """
            mkdir -p ${metricsPath}
            nohup bash -c '
                while [ ! -f "${metricsPath}/${buildId}.stop" ]; do
                    cpu=\$(top -bn1 | grep "Cpu(s)" | awk "{print \\\$2}" | cut -d. -f1)
                    mem=\$(free -m | awk "/Mem:/{print \\\$3}")
                    echo "\$(date +%s),\${cpu},\${mem}" >> "${metricsPath}/${buildId}_resources.csv"
                    sleep 5
                done
            ' > /dev/null 2>&1 &
            echo \$! > "${metricsPath}/${buildId}.pid"
        """
    } else {
        bat """
            if not exist "${metricsPath}" mkdir "${metricsPath}"
            powershell -Command "Start-Job -ScriptBlock { 
                while (-not (Test-Path '${metricsPath}/${buildId}.stop')) { 
                    \$cpu = (Get-Counter '\\Processor(_Total)\\%% Processor Time').CounterSamples.CookedValue
                    \$mem = [math]::Round((Get-Process | Measure-Object WorkingSet -Sum).Sum / 1MB, 0)
                    Add-Content -Path '${metricsPath}/${buildId}_resources.csv' -Value \"\$(Get-Date -UFormat %%s),\$cpu,\$mem\"
                    Start-Sleep -Seconds 5
                }
            }"
        """
    }
    
    echo "✅ Metrics collection started"
    return gitMetrics
}

def stop(Map config = [:]) {
    def buildId = config.buildId ?: env.BUILD_TAG
    def status = config.status ?: currentBuild.result ?: 'SUCCESS'
    def buildType = config.buildType ?: 'debug'
    def metricsPath = config.metricsPath ?: "${env.JENKINS_HOME}/ml-metrics"
    
    echo "📉 Stopping metrics collection for ${buildId}"
    
    // Calculate build duration
    def startTime = env.ML_BUILD_START?.toLong() ?: System.currentTimeMillis()
    def duration = (System.currentTimeMillis() - startTime) / 1000
    
    // Stop resource monitoring
    if (isUnix()) {
        sh """
            touch "${metricsPath}/${buildId}.stop"
            sleep 2
            if [ -f "${metricsPath}/${buildId}.pid" ]; then
                kill \$(cat "${metricsPath}/${buildId}.pid") 2>/dev/null || true
                rm "${metricsPath}/${buildId}.pid"
            fi
        """
    } else {
        bat """
            echo. > "${metricsPath}/${buildId}.stop"
            timeout /t 2 /nobreak > nul
            powershell -Command "Get-Job | Stop-Job -PassThru | Remove-Job"
        """
    }
    
    // Calculate resource averages from collected data
    def cpuAvg = 0.0
    def cpuMax = 0.0
    def memAvg = 0.0
    def memMax = 0.0
    
    def resourceFile = "${metricsPath}/${buildId}_resources.csv"
    if (fileExists(resourceFile)) {
        def content = readFile(resourceFile)
        def lines = content.trim().split('\n')
        def cpuValues = []
        def memValues = []
        
        lines.each { line ->
            def parts = line.split(',')
            if (parts.size() >= 3) {
                try {
                    cpuValues << parts[1].toDouble()
                    memValues << parts[2].toDouble()
                } catch (Exception e) {
                    // Skip invalid lines
                }
            }
        }
        
        if (cpuValues) {
            cpuAvg = cpuValues.sum() / cpuValues.size()
            cpuMax = cpuValues.max()
        }
        if (memValues) {
            memAvg = memValues.sum() / memValues.size()
            memMax = memValues.max()
        }
    }
    
    // Log to training data CSV
    def logger = new MetricsLogger(this)
    logger.log([
        buildId: buildId,
        timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss"),
        branch: env.ML_BRANCH ?: 'unknown',
        buildType: buildType,
        filesChanged: env.ML_FILES_CHANGED?.toInteger() ?: 0,
        linesAdded: env.ML_LINES_ADDED?.toInteger() ?: 0,
        linesDeleted: env.ML_LINES_DELETED?.toInteger() ?: 0,
        depsChanged: env.ML_DEPS_CHANGED?.toInteger() ?: 0,
        cpuAvg: cpuAvg.round(1),
        cpuMax: cpuMax.round(1),
        memoryAvgMb: memAvg.round(0),
        memoryMaxMb: memMax.round(0),
        buildTimeSec: duration.round(0),
        status: status
    ], metricsPath)
    
    def memoryGb = (memAvg / 1024).round(2)
    
    echo "✅ Metrics logged:"
    echo "   ⏱️ Duration: ${duration.round(0)}s"
    echo "   💻 CPU Avg: ${cpuAvg.round(1)}%"
    echo "   💾 Memory Avg: ${memoryGb} GB"
    
    return [
        duration: duration,
        cpu: cpuAvg.round(1),
        memory: memoryGb,
        memoryMb: memAvg.round(0)
    ]
}
