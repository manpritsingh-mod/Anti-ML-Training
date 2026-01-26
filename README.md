# ML Node Selector - Jenkins Shared Library

ML-powered AWS EC2 node selection for Jenkins CI/CD pipelines.

## 🎯 What It Does

Automatically selects the optimal AWS EC2 instance type for each build based on code changes, reducing cloud costs by 40%+.

```
Code Push → ML Analyzes → Predicts Memory → Selects Right Node → Cost Savings!
```

## 📊 Supported AWS Instances

| Label | Instance | Memory | Cost/hr |
|-------|----------|--------|---------|
| `lightweight` | T3a Small | 1 GB | $0.0047 |
| `executor` | T3a Small | 2 GB | $0.0094 |
| `build` | T3a Large | 8 GB | $0.0376 |
| `test` | T3a X Large | 16 GB | $0.0752 |
| `heavytest` | T3a 2X Large | 32 GB | $0.1504 |

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

### Phase 1: Data Collection (First 50+ builds)
```groovy
// In your existing pipelines, add:
collectBuildMetrics.start(buildId: env.BUILD_TAG)
// ... your build ...
collectBuildMetrics.stop(status: 'SUCCESS')
```

### Phase 2: Train Model
```groovy
// Run once you have 50+ builds
trainNodeModel(minBuilds: 50)
```

### Phase 3: Use Predictions
```groovy
def result = selectNode(buildType: 'release')
// result.label = 'build', 'test', etc.
```

## 📈 Expected Results

- **40%+ cost reduction** by right-sizing instances
- **Faster builds** by not waiting for oversized instances
- **Automatic scaling** based on code complexity

## 🛠️ Requirements

- Jenkins 2.x+
- Python 3.8+ on agents
- Python packages: `pip install pandas numpy scikit-learn joblib`

## 📖 Full Documentation

See [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) for complete architecture diagrams and detailed implementation guide.
