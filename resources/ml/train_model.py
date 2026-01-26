#!/usr/bin/env python3
"""
ML Model Training Script for Jenkins Resource Prediction

Trains a Random Forest model to predict:
- CPU usage (%)
- Memory usage (GB)
- Build time (minutes)

Based on git metrics and build context.
"""
import argparse
import json
import os
import sys

try:
    import pandas as pd
    import numpy as np
    from sklearn.ensemble import RandomForestRegressor
    from sklearn.model_selection import train_test_split
    from sklearn.preprocessing import StandardScaler
    from sklearn.pipeline import Pipeline
    from sklearn.metrics import mean_absolute_error, r2_score
    import joblib
except ImportError as e:
    print(f"Error: Missing required library - {e}", file=sys.stderr)
    print("Install with: pip install pandas numpy scikit-learn joblib", file=sys.stderr)
    sys.exit(1)


def load_and_prepare_data(data_path):
    """Load CSV and prepare features"""
    print(f"Loading data from {data_path}...")
    df = pd.read_csv(data_path)
    
    print(f"Total records: {len(df)}")
    
    # Remove failed builds (they have inconsistent metrics)
    if 'status' in df.columns:
        df = df[df['status'] == 'SUCCESS']
        print(f"After filtering SUCCESS only: {len(df)}")
    
    if len(df) < 10:
        raise ValueError(f"Not enough data for training. Need at least 10 successful builds, have {len(df)}")
    
    # Feature engineering
    df['net_lines'] = df['lines_added'] - df['lines_deleted']
    df['total_changes'] = df['lines_added'] + df['lines_deleted']
    df['code_density'] = df['total_changes'] / (df['files_changed'] + 1)
    df['is_main'] = df['branch'].isin(['main', 'master', 'develop']).astype(int)
    df['is_release'] = df['build_type'].isin(['release', 'prodRelease']).astype(int)
    
    # Features to use
    feature_cols = [
        'files_changed', 'lines_added', 'lines_deleted',
        'net_lines', 'total_changes', 'deps_changed',
        'is_main', 'is_release', 'code_density'
    ]
    
    # Targets to predict (convert memory from MB to GB)
    df['memory_gb'] = df['memory_avg_mb'] / 1024
    df['time_minutes'] = df['build_time_sec'] / 60
    
    target_cols = ['cpu_avg', 'memory_gb', 'time_minutes']
    
    # Handle missing values
    X = df[feature_cols].fillna(0)
    y = df[target_cols].fillna(0)
    
    # Remove any remaining problematic rows
    valid_mask = ~(y.isna().any(axis=1) | X.isna().any(axis=1))
    X = X[valid_mask]
    y = y[valid_mask]
    
    print(f"Training features: {feature_cols}")
    print(f"Target variables: {target_cols}")
    
    return X, y, feature_cols


def train_model(X, y):
    """Train Random Forest model"""
    print(f"\nTraining model with {len(X)} samples...")
    
    # Split data: 80% train, 20% test
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )
    
    print(f"Training set: {len(X_train)} samples")
    print(f"Test set: {len(X_test)} samples")
    
    # Create pipeline with scaling and model
    model = Pipeline([
        ('scaler', StandardScaler()),
        ('rf', RandomForestRegressor(
            n_estimators=100,
            max_depth=10,
            min_samples_split=5,
            random_state=42,
            n_jobs=-1
        ))
    ])
    
    # Train
    model.fit(X_train, y_train)
    
    # Evaluate
    y_pred = model.predict(X_test)
    
    # Calculate metrics
    r2 = r2_score(y_test, y_pred)
    mae = mean_absolute_error(y_test, y_pred)
    
    print(f"\n=== Model Performance ===")
    print(f"R² Score: {r2:.4f}")
    print(f"Mean Absolute Error: {mae:.4f}")
    
    # Per-target metrics
    for i, col in enumerate(y.columns):
        col_mae = mean_absolute_error(y_test.iloc[:, i], y_pred[:, i])
        col_r2 = r2_score(y_test.iloc[:, i], y_pred[:, i])
        print(f"  {col}: MAE={col_mae:.2f}, R²={col_r2:.4f}")
    
    metrics = {
        'r2_score': round(r2, 4),
        'mae': round(mae, 4),
        'training_samples': len(X_train),
        'test_samples': len(X_test)
    }
    
    # Feature importance
    rf = model.named_steps['rf']
    importance = dict(zip(X.columns, rf.feature_importances_))
    metrics['feature_importance'] = {k: round(v, 4) for k, v in sorted(importance.items(), key=lambda x: -x[1])}
    
    print(f"\n=== Feature Importance ===")
    for feat, imp in sorted(importance.items(), key=lambda x: -x[1]):
        print(f"  {feat}: {imp:.4f}")
    
    return model, metrics


def main():
    parser = argparse.ArgumentParser(description='Train ML model for node selection')
    parser.add_argument('--data-path', required=True, help='Path to training_data.csv')
    parser.add_argument('--model-path', required=True, help='Directory to save model')
    parser.add_argument('--output-metrics', required=True, help='Path to save metrics.json')
    args = parser.parse_args()
    
    # Validate input file exists
    if not os.path.exists(args.data_path):
        print(f"Error: Data file not found: {args.data_path}", file=sys.stderr)
        sys.exit(1)
    
    # Create output directory
    os.makedirs(args.model_path, exist_ok=True)
    
    try:
        # Load and prepare data
        X, y, feature_cols = load_and_prepare_data(args.data_path)
        
        # Train model
        model, metrics = train_model(X, y)
        
        # Save model
        model_file = os.path.join(args.model_path, 'model.pkl')
        joblib.dump(model, model_file)
        print(f"\n✅ Model saved to {model_file}")
        
        # Save metadata
        metadata = {
            'features': feature_cols,
            'targets': ['cpu_avg', 'memory_gb', 'time_minutes'],
            'metrics': metrics
        }
        
        metadata_file = os.path.join(args.model_path, 'metadata.json')
        with open(metadata_file, 'w') as f:
            json.dump(metadata, f, indent=2)
        print(f"✅ Metadata saved to {metadata_file}")
        
        # Save metrics separately
        with open(args.output_metrics, 'w') as f:
            json.dump(metrics, f, indent=2)
        print(f"✅ Metrics saved to {args.output_metrics}")
        
    except Exception as e:
        print(f"Error during training: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
