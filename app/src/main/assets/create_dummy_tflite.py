import tensorflow as tf

# Create a dummy model
model = tf.keras.Sequential([
    tf.keras.layers.Dense(10, activation='relu', input_shape=(5,)),
    tf.keras.layers.Dense(2)
])

# Compile the model
model.compile(optimizer='adam', loss='mse')

# Create a concrete function
concrete_func = tf.function(lambda x: model(x))
concrete_func = concrete_func.get_concrete_function(tf.TensorSpec(shape=[None, 5], dtype=tf.float32))

# Convert the model to TFLite format
converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete_func])
tflite_model = converter.convert()

# Save the TFLite model to a file
with open("app/src/main/assets/model.tflite", "wb") as f:
    f.write(tflite_model)