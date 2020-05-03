package com.andrideng.plugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;

import android.util.Log;
import android.app.PendingIntent;

import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;

public class HprtPrinter extends CordovaPlugin implements SensorEventListener {
  // - Hprt Printer
  // private static final String ACTION_USB_PERMISSION = "com.andrideng.plugin";
  // private Context thisCon = this.cordova.getActivity().getApplicationContext();

  // private UsbManager mUsbManager = null;
  // private UsbDevice device = null;
  // private PendingIntent mPermissionIntent = PendingIntent.getBroadcast(thisCon, 0, new Intent(ACTION_USB_PERMISSION), 0);

  // - Temperature
  public static int STOPPED = 0;
  public static int STARTING = 1;
  public static int RUNNING = 2;
  public static int ERROR_FAILED_TO_START = 3;

  public long TIMEOUT = 30000;

  int status;
  long timeStamp;
  long lastAccessTime;

  SensorManager sensorManager;
  Sensor sensor;

  Float temperature;
  private CallbackContext callbackContext;
  
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    this.sensorManager = (SensorManager) cordova.getActivity().getSystemService(Context.SENSOR_SERVICE);
    this.sensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
  }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      this.callbackContext = callbackContext;

      if (action.equals("checkTemperature")) {
         this.start();
         return true;
      }
  
      if (action.equals("isDeviceCompatible")) {
         this.isDeviceCompatible();
         return true;
      }

      if("connectUsb".equals(action)) {
        this.connect();
        return true;
      }

      return false;
    }

  public void connect() {
    Toast.makeText(cordova.getActivity(), "We are in the connect function", Toast.LENGTH_LONG).show();
    // mUsbManager = (UsbManager) thisCon.getSystemService(Context.USB_SERVICE);
		// HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		// Log.d("test", deviceList.toString());
		// Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

		// boolean HavePrinter = false;
		// while(deviceIterator.hasNext())
		// {
		// 	device = deviceIterator.next();
		// 	int count = device.getInterfaceCount();
		// 	for (int i = 0; i < count; i++)
		// 	{
		// 		UsbInterface intf = device.getInterface(i);
		// 		if (intf.getInterfaceClass() == 7)
		// 		{
		// 			Log.d("PRINT_TAG", "vendorID--"
		// 					+ device.getVendorId() + "ProductId--"
		// 					+ device.getProductId() + "ProductName: "
		// 					+ device.getProductName() + "DeviceName: "
		// 					+ device.getDeviceName() + device.getManufacturerName()
		// 			);
		// 			HavePrinter=true;
		// 			mUsbManager.requestPermission(device, mPermissionIntent);
		// 		}
		// 	}
		// }
		// // Create the toast
		// if (HavePrinter) {
		// 	Toast.makeText(cordova.getActivity(), "YEAY FOUND IT!", Toast.LENGTH_LONG).show();	
		// } else {
		// 	Toast.makeText(cordova.getActivity(), "TRY AGAIN, YOU CAN DO IT!", Toast.LENGTH_LONG).show();
		// }
  }

  public void isDeviceCompatible() {
    String msg = this.sensor != null ? "This device is compatible" : "Sorry, this device doesn't have a temperature sensor";

    Toast.makeText(cordova.getActivity(), msg, Toast.LENGTH_LONG).show();
    this.callbackContext.success(msg);
  }

  public void start () {
    if ((this.status == RUNNING) || (this.status == STARTING)) {
      return;
    }
    
    if(this.sensor != null) {
      this.sensorManager.registerListener(this, this.sensor, SensorManager.SENSOR_DELAY_NORMAL);
      this.lastAccessTime = System.currentTimeMillis();
      this.status = STARTING;
      Toast.makeText(cordova.getActivity(), "We're detecting the temperature of your surroundings...", Toast.LENGTH_LONG).show();

    } else {
        this.status = ERROR_FAILED_TO_START;
        this.stop();
        Toast.makeText(cordova.getActivity(), "Sorry this device does not have a Temperature sensor" , Toast.LENGTH_LONG).show();
        this.callbackContext.error("Sorry this device does not have a Temperature sensor");
    }
  }

  public void stop () {
    if (this.status != STOPPED) {
        this.sensorManager.unregisterListener(this);
    }
    this.status = STOPPED;
  }

  @Override
  public void onSensorChanged (SensorEvent sensorEvent) {
    try {
      if (sensorEvent.values.length > 0) {
        this.temperature = sensorEvent.values[0];
      }
      this.timeStamp = System.currentTimeMillis();
      this.status = RUNNING;

      if ((this.timeStamp - this.lastAccessTime) > this.TIMEOUT) {
          this.stop();
          callbackContext.error("Sorry there was a timeout, please try again.");
          return;
      }

      String msg = "Your current temperature is " + this.temperature;

      callbackContext.success(msg);
      Toast.makeText(cordova.getActivity(), msg , Toast.LENGTH_LONG).show();

    } catch (Exception e) {
        e.printStackTrace();
        callbackContext.error("Sorry we encountered an issue. Please try again.");
    }
  }

  @Override
  public void onAccuracyChanged (Sensor sensor, int i) {

  }

  @Override
  public void onDestroy () {
      this.stop();
  }

  @Override
  public void onReset () {
      this.stop();
  }
}
