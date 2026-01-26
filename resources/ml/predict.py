#!/usr/bin/env python3
"""
ML Prediction Script for Node Selection

Called by NodePredictor.groovy to get resource predictions.
"""
import argparse
import json
import os
import sys


def engineer_features(context):
    """Create features from build context"""
    files = context.get('filesChanged', 0)
    lines_added = context.get('linesAdded', 0)
    lines_deleted = context.get('linesDeleted', 0)
    deps = context.get('depsChanged', 0)
    branch = context.get('branch', 'main')
    build_type = context.get('buildType', 'debug')
    
    return {
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


def predict_with_model(features, model_path):
    """Make prediction using trained model"""
    try:
        import joblib
        import numpy as np
        
        model = joblib.load(model_path)
        
        # Feature order must match training
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
            'timeMinutes': round(float(prediction[2]), 1),
            'confidence': 85.0,
            'method': 'ml_model'
        }
    except ImportError as e:
        return predict_with_heuristics(features, f"Missing library: {e}")
    except Exception as e:
        return predict_with_heuristics(features, str(e))


def predict_with_heuristics(features, error=None):
    """Fallback heuristic prediction"""
    files = features['files_changed']
    total_lines = features['total_changes']
    deps = features['deps_changed']
    is_release = features['is_release']
    
    # Memory estimation (GB)
    memory = 2.0
    memory += files * 0.05
    memory += total_lines * 0.001
    memory += deps * 1.5
    if is_release:
        memory *= 1.5
    memory = max(1.0, min(memory, 30.0))  # Clamp between 1-30GB
    
    # CPU estimation
    cpu = 30 + files * 0.5 + total_lines * 0.01
    cpu = max(10, min(cpu, 95))
    
    # Time estimation (minutes)
    time = 5 + files * 0.2 + total_lines * 0.005 + deps * 2
    if is_release:
        time *= 1.3
    time = max(2, min(time, 60))
    
    result = {
        'cpu': round(cpu, 1),
        'memoryGb': round(memory, 1),
        'timeMinutes': round(time, 1),
        'confidence': 60.0,
        'method': 'heuristic'
    }
    
    if error:
        result['fallback_reason'] = error
    
    return result


def main():
    parser = argparse.ArgumentParser(description='ML Node Selection Prediction')
    parser.add_argument('--input', required=True, help='Input JSON file with build context')
    parser.add_argument('--model', required=True, help='Path to trained model.pkl')
    args = parser.parse_args()
    
    # Load context
    try:
        with open(args.input, 'r') as f:
            context = json.load(f)
    except Exception as e:
        print(json.dumps({
            'cpu': 50.0,
            'memoryGb': 4.0,
            'timeMinutes': 10.0,
            'confidence': 0.0,
            'method': 'error',
            'error': f'Failed to load input: {e}'
        }))
        return
    
    # Engineer features
    features = engineer_features(context)
    
    # Predict
    if os.path.exists(args.model):
        result = predict_with_model(features, args.model)
    else:
        result = predict_with_heuristics(features, "Model file not found")
    
    print(json.dumps(result))


if __name__ == '__main__':
    main()
