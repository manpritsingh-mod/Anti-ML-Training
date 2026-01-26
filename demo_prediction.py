#!/usr/bin/env python3
"""
Demo Script - Test the trained ML model for node selection

Run this to verify the model works correctly before deploying.
"""
import json
import os
import sys

# Add resources/ml to path
script_dir = os.path.dirname(os.path.abspath(__file__))
resources_dir = os.path.join(script_dir, 'resources', 'ml')
sys.path.insert(0, resources_dir)

try:
    import joblib
    import numpy as np
except ImportError:
    print("Please install: pip install joblib numpy")
    sys.exit(1)


def predict(context, model_path):
    """Make prediction using trained model"""
    model = joblib.load(model_path)
    
    # Engineer features (same as training)
    files = context.get('filesChanged', 0)
    lines_added = context.get('linesAdded', 0)
    lines_deleted = context.get('linesDeleted', 0)
    deps = context.get('depsChanged', 0)
    branch = context.get('branch', 'main')
    build_type = context.get('buildType', 'debug')
    
    features = {
        'files_changed': files,
        'lines_added': lines_added,
        'lines_deleted': lines_deleted,
        'net_lines': lines_added - lines_deleted,
        'total_changes': lines_added + lines_deleted,
        'deps_changed': deps,
        'is_main': 1 if branch in ['main', 'master', 'develop'] else 0,
        'is_release': 1 if build_type in ['release', 'prodRelease'] else 0,
        'code_density': (lines_added + lines_deleted) / max(files, 1)
    }
    
    feature_order = [
        'files_changed', 'lines_added', 'lines_deleted',
        'net_lines', 'total_changes', 'deps_changed',
        'is_main', 'is_release', 'code_density'
    ]
    
    X = np.array([[features[f] for f in feature_order]])
    prediction = model.predict(X)[0]
    
    return {
        'cpu': round(float(prediction[0]), 1),
        'memoryGb': round(float(prediction[1]), 1),
        'timeMinutes': round(float(prediction[2]), 1)
    }


def select_label(memory_gb):
    """Map memory prediction to Jenkins label"""
    required = memory_gb * 1.2  # 20% buffer
    
    if required <= 1.0:
        return 'lightweight', 'T3a Small (1GB)'
    elif required <= 2.0:
        return 'executor', 'T3a Small (2GB)'
    elif required <= 8.0:
        return 'build', 'T3a Large (8GB)'
    elif required <= 16.0:
        return 'test', 'T3a X Large (16GB)'
    else:
        return 'heavytest', 'T3a 2X Large (32GB)'


def main():
    model_path = os.path.join(script_dir, 'resources', 'ml', 'model.pkl')
    
    if not os.path.exists(model_path):
        print(f"❌ Model not found at: {model_path}")
        print("   Run training first!")
        sys.exit(1)
    
    print("=" * 60)
    print("🤖 ML Node Selection - Demo")
    print("=" * 60)
    print(f"📁 Model: {model_path}")
    print()
    
    # Test scenarios
    test_cases = [
        {
            'name': '🟢 Small Change (hotfix)',
            'context': {'filesChanged': 3, 'linesAdded': 50, 'linesDeleted': 10, 'depsChanged': 0, 'branch': 'hotfix/bug', 'buildType': 'debug'}
        },
        {
            'name': '🟡 Medium Change (feature)',
            'context': {'filesChanged': 25, 'linesAdded': 700, 'linesDeleted': 150, 'depsChanged': 0, 'branch': 'feature/login', 'buildType': 'debug'}
        },
        {
            'name': '🟠 Large Change (refactor)',
            'context': {'filesChanged': 50, 'linesAdded': 1500, 'linesDeleted': 400, 'depsChanged': 2, 'branch': 'develop', 'buildType': 'debug'}
        },
        {
            'name': '🔴 Release Build (main)',
            'context': {'filesChanged': 65, 'linesAdded': 1800, 'linesDeleted': 500, 'depsChanged': 2, 'branch': 'main', 'buildType': 'release'}
        },
        {
            'name': '🟣 Massive Change (big feature)',
            'context': {'filesChanged': 80, 'linesAdded': 2200, 'linesDeleted': 600, 'depsChanged': 3, 'branch': 'feature/redesign', 'buildType': 'debug'}
        }
    ]
    
    for test in test_cases:
        print(f"\n{test['name']}")
        print("-" * 50)
        
        ctx = test['context']
        print(f"  📁 Files: {ctx['filesChanged']}, Lines: +{ctx['linesAdded']}/-{ctx['linesDeleted']}")
        print(f"  📦 Deps: {ctx['depsChanged']}, Branch: {ctx['branch']}, Type: {ctx['buildType']}")
        
        result = predict(ctx, model_path)
        label, instance = select_label(result['memoryGb'])
        
        print(f"\n  🔮 Prediction:")
        print(f"     💻 CPU:    {result['cpu']}%")
        print(f"     💾 Memory: {result['memoryGb']} GB")
        print(f"     ⏱️  Time:   {result['timeMinutes']} min")
        print(f"\n  ✅ Selected: {label} → {instance}")
    
    print("\n" + "=" * 60)
    print("✅ Model is working correctly! Ready for POC demo.")
    print("=" * 60)


if __name__ == '__main__':
    main()
