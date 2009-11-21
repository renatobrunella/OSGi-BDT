package uk.co.brunella.osgi.bdt.example.sensor.temperature;

public interface TemperatureSensor {

  final static String SENSOR_NAME = "SENSOR_NAME";
  final static String MIN_TEMPERATURE = "MIN_TEMPERATURE";
  final static String MAX_TEMPERATURE = "MAX_TEMPERATURE";
  
  double readTemperature();
}
