# ML Node Selector - Jenkins Shared Library

ML-powered AWS EC2 node selection for Jenkins CI/CD pipelines.

## 🎯 What It Does

Automatically selects the optimal AWS EC2 instance type for each build based on code changes using Machine Learning.

```
Code Push → ML Analyzes → Predicts Memory → Selects Right Node
```

## 📊 Supported AWS Instances

| Label | Instance | Memory |
|-------|----------|--------|
| `lightweight` | T3a Small | 1 GB |
| `executor` | T3a Small | 2 GB |
| `build` | T3a Large | 8 GB |
| `test` | T3a X Large | 16 GB |
| `heavytest` | T3a 2X Large | 32 GB |

## 🚀 Quick Start

### 1. Configure in Jenkins

Go to **Manage Jenkins** → **Configure System** → **Global Pipeline Libraries**:

- Name: `ml-node-selector`
- Default version: `main`
- Source: Git repository URL

### 2. Use in Jenkinsfile

```groovy
@Library('ml-node-selector') _

pipeline {
    agent none
    
    stages {
        stage('Select Node') {
            agent { label 'master' }
            steps {
                script {
                    def result = selectNode(buildType: 'debug')
                    echo "Selected: ${result.label}"
                }
            }
        }
        
        stage('Build') {
            agent { label "${env.ML_SELECTED_LABEL}" }
            steps {
                collectBuildMetrics.start()
                sh './gradlew build'
                collectBuildMetrics.stop()
            }
        }
    }
}
```

## 📁 Project Structure

```
ml-node-selector/
├── vars/
│   ├── selectNode.groovy         # Main prediction step
│   ├── collectBuildMetrics.groovy # Collect training data
│   └── trainNodeModel.groovy     # Train/retrain model
├── src/org/ml/nodeselection/
│   ├── GitAnalyzer.groovy        # Git metrics extraction
│   ├── LabelMapper.groovy        # Memory → Label mapping
│   ├── NodePredictor.groovy      # ML prediction wrapper
│   └── MetricsLogger.groovy      # CSV logging
├── resources/ml/
│   ├── predict.py                # Python prediction script
│   ├── train_model.py            # Python training script
│   └── requirements.txt          # Python dependencies
└── docs/
    └── IMPLEMENTATION_PLAN.md    # Full architecture docs
```

## 🔧 How It Works

### Phase 1: Train Model with Synthetic Data
```groovy
// Model is pre-trained with synthetic data (97.33% accuracy)
// Ready to use immediately for POC
```

### Phase 2: Use Predictions
```groovy
def result = selectNode(buildType: 'release')
// result.label = 'build', 'test', etc.
```

### Phase 3: Retrain with Real Data (Optional)
```groovy
// Collect real build metrics
collectBuildMetrics.start(buildId: env.BUILD_TAG)
// ... your build ...
collectBuildMetrics.stop(status: 'SUCCESS')

// Retrain when you have 50+ builds
trainNodeModel(minBuilds: 50)
```

## 📈 Model Performance

- **R² Score:** 97.33% (Accuracy)
- **Mean Absolute Error:** 1.02
- **Trained on:** 60 synthetic builds

## 🛠️ Requirements

- Jenkins 2.x+
- Python 3.8+ on agents
- Python packages: `pip install pandas numpy scikit-learn joblib`

## 📖 Full Documentation

See [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) for complete architecture diagrams and detailed implementation guide.
