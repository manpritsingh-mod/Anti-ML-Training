#!/usr/bin/env groovy
/**
 * selectNode - ML-powered AWS node selection
 * 
 * Usage in Jenkinsfile:
 *   def result = selectNode(buildType: 'debug')
 *   echo "Selected label: ${result.label}"
 *   
 *   // Then use in agent directive:
 *   agent { label result.label }
 */

import org.ml.nodeselection.GitAnalyzer
import org.ml.nodeselection.NodePredictor
import org.ml.nodeselection.LabelMapper

def call(Map config = [:]) {
    def buildType = config.buildType ?: 'debug'
    def modelPath = config.modelPath ?: "${env.JENKINS_HOME}/ml-models"
    
    echo "🤖 ML Node Selection Starting..."
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    // Step 1: Analyze git changes
    def gitAnalyzer = new GitAnalyzer(this)
    def gitMetrics = gitAnalyzer.analyze()
    
    echo "📊 Code Analysis:"
    echo "   📁 Files changed: ${gitMetrics.filesChanged}"
    echo "   ➕ Lines added: ${gitMetrics.linesAdded}"
    echo "   ➖ Lines deleted: ${gitMetrics.linesDeleted}"
    echo "   📦 Dependencies changed: ${gitMetrics.depsChanged}"
    echo "   🌿 Branch: ${gitMetrics.branch}"
    
    // Step 2: Make prediction
    def predictor = new NodePredictor(this)
    def prediction = predictor.predict([
        filesChanged: gitMetrics.filesChanged,
        linesAdded: gitMetrics.linesAdded,
        linesDeleted: gitMetrics.linesDeleted,
        depsChanged: gitMetrics.depsChanged,
        branch: gitMetrics.branch,
        buildType: buildType
    ], modelPath)
    
    echo ""
    echo "🔮 ML Prediction:"
    echo "   💻 Predicted CPU: ${prediction.cpu}%"
    echo "   💾 Predicted Memory: ${prediction.memoryGb} GB"
    echo "   ⏱️ Predicted Time: ${prediction.timeMinutes} min"
    
    // Step 3: Map to AWS label
    def mapper = new LabelMapper()
    def label = mapper.getLabel(prediction.memoryGb)
    def instanceType = mapper.getInstanceType(prediction.memoryGb)
    
    echo ""
    echo "✅ Selected Configuration:"
    echo "   🏷️ Jenkins Label: ${label}"
    echo "   ☁️ AWS Instance: ${instanceType}"
    echo "   📊 Confidence: ${prediction.confidence}%"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    // Store in environment for use
    env.ML_SELECTED_LABEL = label
    env.ML_PREDICTED_MEMORY = prediction.memoryGb.toString()
    env.ML_PREDICTED_CPU = prediction.cpu.toString()
    env.ML_PREDICTED_TIME = prediction.timeMinutes.toString()
    
    return [
        label: label,
        instanceType: instanceType,
        predictedMemoryGb: prediction.memoryGb,
        predictedCpu: prediction.cpu,
        predictedTimeMinutes: prediction.timeMinutes,
        confidence: prediction.confidence,
        gitMetrics: gitMetrics
    ]
}
