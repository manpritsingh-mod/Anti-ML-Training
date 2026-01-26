package org.ml.nodeselection

/**
 * MetricsLogger - Log build metrics to CSV for ML training
 */
class MetricsLogger implements Serializable {
    
    def steps
    
    // CSV header columns
    static final String CSV_HEADER = "build_id,timestamp,branch,build_type,files_changed,lines_added,lines_deleted,deps_changed,cpu_avg,cpu_max,memory_avg_mb,memory_max_mb,build_time_sec,status"
    
    MetricsLogger(steps) {
        this.steps = steps
    }
    
    /**
     * Log build metrics to training CSV
     */
    void log(Map metrics, String outputPath) {
        def csvFile = "${outputPath}/training_data.csv"
        
        // Ensure output directory exists
        if (steps.isUnix()) {
            steps.sh "mkdir -p '${outputPath}'"
        } else {
            steps.bat "if not exist \"${outputPath}\" mkdir \"${outputPath}\""
        }
        
        // Check if header needed
        def needsHeader = !steps.fileExists(csvFile)
        
        // Build CSV row
        def row = [
            metrics.buildId ?: 'unknown',
            metrics.timestamp ?: new Date().format("yyyy-MM-dd'T'HH:mm:ss"),
            metrics.branch ?: 'unknown',
            metrics.buildType ?: 'debug',
            metrics.filesChanged ?: 0,
            metrics.linesAdded ?: 0,
            metrics.linesDeleted ?: 0,
            metrics.depsChanged ?: 0,
            metrics.cpuAvg ?: 0,
            metrics.cpuMax ?: 0,
            metrics.memoryAvgMb ?: 0,
            metrics.memoryMaxMb ?: 0,
            metrics.buildTimeSec ?: 0,
            metrics.status ?: 'UNKNOWN'
        ].join(',')
        
        if (needsHeader) {
            // Write header + first row
            steps.writeFile(file: csvFile, text: "${CSV_HEADER}\n${row}\n")
        } else {
            // Append row
            if (steps.isUnix()) {
                steps.sh "echo '${row}' >> '${csvFile}'"
            } else {
                steps.bat "echo ${row} >> \"${csvFile}\""
            }
        }
        
        steps.echo "📝 Logged metrics to ${csvFile}"
    }
    
    /**
     * Get count of training samples
     */
    int getTrainingDataCount(String metricsPath) {
        def csvFile = "${metricsPath}/training_data.csv"
        
        if (!steps.fileExists(csvFile)) {
            return 0
        }
        
        try {
            def content = steps.readFile(csvFile)
            def lines = content.trim().split('\n')
            return Math.max(0, lines.size() - 1)  // Minus header
        } catch (Exception e) {
            return 0
        }
    }
}
