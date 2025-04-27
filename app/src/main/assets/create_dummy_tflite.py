import tensorflow as tf

# Create a simple model.
model = tf.keras.models.Sequential([
  tf.keras.layers.Dense(10, activation='relu', input_shape=(10,)),
  tf.keras.layers.Dense(1)
])

# Convert the model.
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Save the model.
with open('app/src/main/assets/model.tflite', 'wb') as f:
  f.write(tflite_model)