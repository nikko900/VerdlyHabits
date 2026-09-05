import os
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras

def create_smart_nudge_model(csv_path=None):
    """
    Inputs: timeOfDay, dayOfWeek, activityState, timeSinceLastCompletion
    Output: Engagement Probability (float between 0 and 1)
    """
    if csv_path and os.path.exists(csv_path):
        print(f"Loading real data for Smart Nudge Model from {csv_path}...")
        df = pd.read_csv(csv_path)
        
        X_train = df[['timeOfDay', 'dayOfWeek', 'activityState', 'timeSinceLastCompletion']].values.astype(np.float32)
        y_train = df['didEngage'].values.astype(np.float32)
    else:
        print("Generating synthetic data for Smart Nudge Model...")
        X_train = np.random.rand(1000, 4).astype(np.float32)
        y_train = np.where(np.sum(X_train, axis=1) > 2.0, 0.8, 0.2).astype(np.float32)

    print("Building and training Smart Nudge Model...")
    model = keras.Sequential([
        keras.layers.Dense(8, activation='relu', input_shape=(4,)),
        keras.layers.Dense(4, activation='relu'),
        keras.layers.Dense(1, activation='sigmoid') 
    ])
    model.compile(optimizer='adam', loss='mse')
    model.fit(X_train, y_train, epochs=5, verbose=0)
    
    return model

def create_relapse_prediction_model(csv_path=None):
    """
    Inputs: currentStreakLength, completionRatio, responseTime
    Output: Churn Probability (float between 0 and 1)
    """
    if csv_path and os.path.exists(csv_path):
        print(f"Loading real data for Relapse Prediction Model from {csv_path}...")
        df = pd.read_csv(csv_path)
        
        X_train = df[['currentStreakLength', 'completionRatio', 'responseTime']].values.astype(np.float32)
        y_train = df['didRelapse'].values.astype(np.float32)
    else:
        print("Generating synthetic data for Relapse Prediction Model...")
        X_train = np.random.rand(1000, 3).astype(np.float32)
        y_train = np.where((X_train[:, 0] < 0.3) & (X_train[:, 1] < 0.3), 0.9, 0.1).astype(np.float32)

    print("Building and training Relapse Prediction Model...")
    model = keras.Sequential([
        keras.layers.Dense(8, activation='relu', input_shape=(3,)),
        keras.layers.Dense(4, activation='relu'),
        keras.layers.Dense(1, activation='sigmoid') 
    ])
    model.compile(optimizer='adam', loss='mse')
    model.fit(X_train, y_train, epochs=5, verbose=0)
    
    return model

def create_addiction_prediction_model(csv_path=None):
    """
    Inputs: screenTimeHours
    Output: Addiction Score (float between 0 and 1)
    """
    if csv_path and os.path.exists(csv_path):
        print(f"Loading real data for Addiction Prediction Model from {csv_path}...")
        df = pd.read_csv(csv_path)
    
        
        X_train = df[['screenTimeHours']].values.astype(np.float32)
        y_train = df['addictionLabel'].values.astype(np.float32)
    else:
        print("Generating synthetic data for Addiction Prediction Model...")
    
        X_train = (np.random.rand(1000, 1) * 12).astype(np.float32)
        
        y_train = np.where(X_train[:, 0] > 6.0, 0.8, 0.2).astype(np.float32)

    print("Building and training Addiction Prediction Model...")
    model = keras.Sequential([
        keras.layers.Dense(8, activation='relu', input_shape=(1,)),
        keras.layers.Dense(4, activation='relu'),
        keras.layers.Dense(1, activation='sigmoid')
    ])
    model.compile(optimizer='adam', loss='mse')
    model.fit(X_train, y_train, epochs=5, verbose=0)
    
    return model

def export_to_tflite(model, output_path):
    print(f"Exporting model to {output_path}...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    print("Export complete.")

if __name__ == "__main__":
    assets_dir = "../app/src/main/assets"
    

    nudge_csv = "nudge_dataset.csv"
    relapse_csv = "relapse_dataset.csv"
    
    # 1. Smart Nudge Model
    smart_nudge_model = create_smart_nudge_model(csv_path=nudge_csv)
    export_to_tflite(smart_nudge_model, os.path.join(assets_dir, "smart_nudge_model.tflite"))
    
    print("-" * 30)
    
    # Relapse Prediction Model
    relapse_model = create_relapse_prediction_model(csv_path=relapse_csv)
    export_to_tflite(relapse_model, os.path.join(assets_dir, "relapse_prediction_model.tflite"))
    
    print("-" * 30)
    
   # Addiction Prediction Model
    addiction_csv = "addiction_dataset.csv"
    addiction_model = create_addiction_prediction_model(csv_path=addiction_csv)
    export_to_tflite(addiction_model, os.path.join(assets_dir, "addiction_prediction_model.tflite"))
    
    print("Successfully trained and exported all models to the Android assets directory!")
