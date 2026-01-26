#!/usr/bin/env groovy
/**
 * trainNodeModel - Trigger ML model training/retraining
 * 
 * Usage:
 *   trainNodeModel(minBuilds: 50)
 */

def call(Map config = [:]) {
    def metricsPath = config.metricsPath ?: "${env.JENKINS_HOME}/ml-metrics"
    def modelPath = config.modelPath ?: "${env.JENKINS_HOME}/ml-models"
    def minBuilds = config.minBuilds ?: 50
    
    echo "🧠 ML Model Training Starting..."
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    // Check if we have enough data
    def dataFile = "${metricsPath}/training_data.csv"
    def lineCount = 0
    
    if (fileExists(dataFile)) {
        def content = readFile(dataFile)
        lineCount = content.trim().split('\n').size() - 1  // Minus header
    }
    
    echo "📊 Training data: ${lineCount} builds available"
    
    if (lineCount < minBuilds) {
        echo "⚠️ Not enough training data. Have ${lineCount} builds, need ${minBuilds}."
        echo "   Keep running builds with collectBuildMetrics to gather more data."
        return [trained: false, reason: "Insufficient data: ${lineCount}/${minBuilds}"]
    }
    
    // Get and write training script
    def trainScript = libraryResource('ml/train_model.py')
    writeFile(file: 'train_model.py', text: trainScript)
    
    // Create model output directory
    if (isUnix()) {
        sh "mkdir -p ${modelPath}"
    } else {
        bat "if not exist \"${modelPath}\" mkdir \"${modelPath}\""
    }
    
    // Run Python training
    echo "🔄 Training model..."
    
    def exitCode
    if (isUnix()) {
        exitCode = sh(
            script: """
                python3 train_model.py \\
                    --data-path "${dataFile}" \\
                    --model-path "${modelPath}" \\
                    --output-metrics "${modelPath}/metrics.json"
            """,
            returnStatus: true
        )
    } else {
        exitCode = bat(
            script: """
                python train_model.py ^
                    --data-path "${dataFile}" ^
                    --model-path "${modelPath}" ^
                    --output-metrics "${modelPath}/metrics.json"
            """,
            returnStatus: true
        )
    }
    
    if (exitCode != 0) {
        echo "❌ Model training failed!"
        return [trained: false, reason: "Training script failed with exit code ${exitCode}"]
    }
    
    // Read and display metrics
    def metricsFile = "${modelPath}/metrics.json"
    if (fileExists(metricsFile)) {
        def metricsJson = readFile(metricsFile)
        def metrics = readJSON(text: metricsJson)
        
        echo ""
        echo "✅ Model trained successfully!"
        echo "   📈 R² Score: ${metrics.r2_score}"
        echo "   📉 Mean Absolute Error: ${metrics.mae}"
        echo "   📊 Training samples: ${metrics.training_samples}"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        
        return [
            trained: true,
            metrics: metrics
        ]
    }
    
    return [trained: true, metrics: null]
}
