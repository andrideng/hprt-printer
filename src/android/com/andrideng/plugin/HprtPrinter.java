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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;
import android.app.PendingIntent;

import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;

import HPRTAndroidSDK.HPRTPrinterHelper;
import HPRTAndroidSDK.IPort;
import print.Print;
import print.PublicFunction;
// import rx.functions.Action1;

public class HprtPrinter extends CordovaPlugin implements SensorEventListener {
  // - Hprt Printer
  private static final String ACTION_USB_PERMISSION = "com.andrideng.plugin";
  private Context thisCon;

  private UsbManager mUsbManager = null;
  private UsbDevice device = null;
  private PendingIntent mPermissionIntent;

  private ExecutorService executorService;
  private PublicAction PAct = null;

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
    this.thisCon = cordova.getActivity().getApplicationContext();
    this.mPermissionIntent = PendingIntent.getBroadcast(this.thisCon, 0, new Intent(ACTION_USB_PERMISSION), 0);
    this.executorService = Executors.newSingleThreadExecutor();
    this.PAct = new PublicAction(this.thisCon);
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

      if ("connectUsb".equals(action)) {
        this.connect();
        return true;
      }

      if ("printSample".equals(action)) {
        this.printSample();
        return true;
      }

      return false;
    }

  public void connect() {
    Toast.makeText(cordova.getActivity(), "We are in the connect function", Toast.LENGTH_LONG).show();
    mUsbManager = (UsbManager) thisCon.getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		Log.d("test", deviceList.toString());
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

		boolean HavePrinter = false;
		while(deviceIterator.hasNext())
		{
			device = deviceIterator.next();
			int count = device.getInterfaceCount();
			for (int i = 0; i < count; i++)
			{
				UsbInterface intf = device.getInterface(i);
				if (intf.getInterfaceClass() == 7)
				{
					Log.d("PRINT_TAG", "vendorID--"
							+ device.getVendorId() + "ProductId--"
							+ device.getProductId() + "ProductName: "
							+ device.getProductName() + "DeviceName: "
							+ device.getDeviceName() + device.getManufacturerName()
					);
					HavePrinter=true;
					mUsbManager.requestPermission(device, mPermissionIntent);
				}
			}
		}
		// Create the toast
		if (HavePrinter) {
			Toast.makeText(cordova.getActivity(), "YEAY FOUND IT!", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(cordova.getActivity(), "TRY AGAIN, YOU CAN DO IT!", Toast.LENGTH_LONG).show();
		}
  }
  
  private void printSample() {
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Print.Initialize();
//					PAct.LanguageEncode();
//					InputStream open = getResources().getAssets().open("test01.jpg");
//					Bitmap bitmap = BitmapFactory.decodeStream(open);
//					Print.PrintBitmap(bitmap,  (byte)1, (byte)0,200);
					PAct.BeforePrintAction();
					String ReceiptLines [] = {
            "      Electronics\n",
            "     Technology Co., Ltd.\n",
            "\n",
            "TEL +86-(0)592-5885991\n",
            "                             C#2\n",
            "         2013-08-20\n",
            "LPA                   $333.00\n",
            "--------------------------------\n",
            "LPB                  $444.00\n",
            "--------------------------------\n",
            "LPC                    $555.00\n",
            "--------------------------------\n",
            "\n",
            "Before adding tax       $1332.00\n",
            "tax   5.0%                $66.60\n",
            "--------------------------------\n",
            "total                   $1398.60\n",
            "Customer's payment      $1400.00\n",
            "Change                     $1.40\n"
          };
					for(int i=0;i<ReceiptLines.length;i++) {
            Print.PrintText(ReceiptLines[i]);
          }
					PAct.AfterPrintAction();
//					InputStream open2 = getResources().getAssets().open("test02.png");
//					Bitmap bitmap2 = BitmapFactory.decodeStream(open2);
//					Print.PrintBitmap(bitmap2,  (byte)1, (byte)0,200);
//					HPRTPrinterHelper.SelectCharacterFont((byte) 1);
//					PublicFunction PFunz=new PublicFunction(Main4Activity.this);
//					String sLanguage="Iran"; String sLEncode="iso-8859-6";
//					int intLanguageNum=56; sLEncode=PFunz.getLanguageEncode(sLanguage);
//					intLanguageNum= PFunz.getCodePageIndex(sLanguage); HPRTPrinterHelper.SetCharacterSet((byte)intLanguageNum);
//					HPRTPrinterHelper.LanguageEncode=sLEncode;
					//HPRTPrinterHelper.SetCharacterSet Returns -3 HPRTPrinterHelper.PrintText("این یک پیام برای تست میباشد.\r\n");
					//SDK下发指令设置codepage
//					HPRTPrinterHelper.SetCharacterSet((byte)56);
//					//设置编码
//					HPRTPrinterHelper.LanguageEncode="iso-8859-6";
//					HPRTPrinterHelper.PrintText("این یک پیام برای تست میباشد.\r\n");

				}
				catch(Exception e)
				{
					Log.e("Print", (new StringBuilder("Activity_Main --> PrintSampleReceipt ")).append(e.getMessage()).toString());
				}
			}
		});
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
