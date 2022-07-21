package leesiongchan.reactnativeescpos;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.os.Handler;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.github.escposjava.print.NetworkPrinter;
import io.github.escposjava.print.Printer;
import io.github.escposjava.print.exceptions.BarcodeSizeError;
import io.github.escposjava.print.exceptions.QRCodeException;
import leesiongchan.reactnativeescpos.helpers.EscPosHelper;
import leesiongchan.reactnativeescpos.helpers.PrinterNotFoundException;
import android.util.Log;

public class EscPosModule extends ReactContextBaseJavaModule {
    public static final int DISCONNECT_TIMEOUT = 20000;
    public static final String PRINTING_SIZE_58_MM = "PRINTING_SIZE_58_MM";
    public static final String PRINTING_SIZE_76_MM = "PRINTING_SIZE_76_MM";
    public static final String PRINTING_SIZE_80_MM = "PRINTING_SIZE_80_MM";
    public static final String BLUETOOTH_CONNECTED = "BLUETOOTH_CONNECTED";
    public static final String BLUETOOTH_DISCONNECTED = "BLUETOOTH_DISCONNECTED";
    public static final String BLUETOOTH_DEVICE_FOUND = "BLUETOOTH_DEVICE_FOUND";
    public static final String ERROR_PRINTER_CONNECTION = "Could not connect to printer";
    public static final String ERROR_PRINTER_MISSING_ARGUMENTS = "Missing connection address or printer type";

    private final ReactApplicationContext reactContext;
    private ReadableMap config;
    private ScanManager scanManager;
    private static Map<String, PrinterService> printerServices = new HashMap<>();

    enum BluetoothEvent {
        CONNECTED, DISCONNECTED, DEVICE_FOUND, NONE
    }

    public EscPosModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        scanManager = new ScanManager(reactContext, BluetoothAdapter.getDefaultAdapter());
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(PRINTING_SIZE_58_MM, PRINTING_SIZE_58_MM);
        constants.put(PRINTING_SIZE_76_MM, PRINTING_SIZE_76_MM);
        constants.put(PRINTING_SIZE_80_MM, PRINTING_SIZE_80_MM);
        constants.put(BLUETOOTH_CONNECTED, BluetoothEvent.CONNECTED.name());
        constants.put(BLUETOOTH_DISCONNECTED, BluetoothEvent.DISCONNECTED.name());
        constants.put(BLUETOOTH_DEVICE_FOUND, BluetoothEvent.DEVICE_FOUND.name());
        return constants;
    }

    @Override
    public String getName() {
        return "EscPos";
    }

    @ReactMethod
    public void connect(String address, int port, String type, Promise promise) {
         try {
            if (address.isEmpty() || type.isEmpty()) {
                 throw new IllegalArgumentException(ERROR_PRINTER_MISSING_ARGUMENTS);
            }
            Printer printer;
            if (printerServices.get(address) == null) {
                if ("bluetooth".equals(type)) {
                    printer = new BluetoothPrinter(address);
                } else {
                    printer = new NetworkPrinter(address, port);
                }
                if (Objects.isNull(printer)) {
                    throw new PrinterNotFoundException(ERROR_PRINTER_CONNECTION);
                }
                PrinterService printerService = new PrinterService(printer);
                if (Objects.isNull(printerService)) {
                    throw new PrinterNotFoundException(ERROR_PRINTER_CONNECTION);
                }
                printerService.setContext(reactContext);
                printerServices.put(address, printerService);
            }
            promise.resolve(true);
        } catch (IOException | PrinterNotFoundException e) {
            promise.reject(new PrinterNotFoundException(ERROR_PRINTER_CONNECTION));
        } catch (IllegalArgumentException e) {
            promise.reject(e);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void cutPart(String address, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.cutPart();
            promise.resolve(true);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void cutFull(String address, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.cutFull();
            promise.resolve(true);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void lineBreak(String address, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.lineBreak();
            promise.resolve(true);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void print(String address, String text, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.print(text);
            promise.resolve(true);
        } catch (UnsupportedEncodingException e) {
            promise.reject(e);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void printLn(String address, String text, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.printLn(text);
            promise.resolve(true);
        } catch (UnsupportedEncodingException e) {
            promise.reject(e);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void printBarcode(String address, String code, String bc, int width, int height, String pos, String font, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.printBarcode(code, bc, width, height, pos, font);
            promise.resolve(true);
        } catch (BarcodeSizeError e) {
            promise.reject(e);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void printDesign(String address, String text, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.printDesign(text);
            promise.resolve(true);
        } catch (IOException e) {
            promise.reject(e);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void printImage(String address, String filePath, Promise promise) {
        try { 
            PrinterService printerService = printerServices.get(address);
            if (printerService.timerId != null) {
                EscPosHelper.clearTimeout(printerService.timerId);
            }

            printerService.printImage(filePath);
            promise.resolve(true);
        } catch (IOException e) {
            promise.reject(e);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    // @ReactMethod
    // public void printImageWithOffset(String filePath, int widthOffet, Promise promise) {
    //     try {
    //         printerService.printImage(filePath, widthOffet);
    //         promise.resolve(true);
    //     } catch (IOException e) {
    //         promise.reject(e);
    //     }
    // }

    @ReactMethod
    public void printQRCode(String address, String value, int size, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.printQRCode(value, size);
            promise.resolve(true);
        } catch (QRCodeException e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void printSample(String address, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.printSample();
            promise.resolve(true);
        } catch (IOException e) {
            promise.reject(e);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void write(String address, byte[] command, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.write(command);
            promise.resolve(true);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void setCharCode(String address, String code, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.setCharCode(code);
            promise.resolve(true);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void setTextDensity(String address, int density, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.setTextDensity(density);
            promise.resolve(true);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void setPrintingSize(String address, String printingSize, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            int charsOnLine;
            int printingWidth;

            switch (printingSize) {
                case PRINTING_SIZE_80_MM:
                    charsOnLine = LayoutBuilder.CHARS_ON_LINE_80_MM;
                    printingWidth = PrinterService.PRINTING_WIDTH_80_MM;
                    break;
                case PRINTING_SIZE_76_MM:
                    charsOnLine = LayoutBuilder.CHARS_ON_LINE_76_MM;
                    printingWidth = PrinterService.PRINTING_WIDTH_76_MM;
                    break;
                case PRINTING_SIZE_58_MM:
                default:
                    charsOnLine = LayoutBuilder.CHARS_ON_LINE_58_MM;
                    printingWidth = PrinterService.PRINTING_WIDTH_58_MM;
            }
            printerService.setCharsOnLine(charsOnLine);
            printerService.setPrintingWidth(printingWidth);
            promise.resolve(true);
        } catch (Throwable e) {
            promise.reject(e);
        }
            
    }

    public void beep(String address, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.beep();
            promise.resolve(true);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    // @ReactMethod
    // public void setConfig(ReadableMap config) {
    //     this.config = config;
    // }

    public void kickCashDrawerPin2(String address, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.kickCashDrawerPin2();
            promise.resolve(true);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void kickCashDrawerPin5(String address, Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);
            printerService.kickCashDrawerPin5();
            promise.resolve(true);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    // @ReactMethod
    // public void connectBluetoothPrinter(String address, Promise promise) {
    //     try {
    //         if (!"bluetooth".equals(config.getString("type"))) {
    //             promise.reject("config.type is not a bluetooth type");
    //         }
    //         Printer printer = new BluetoothPrinter(address);
    //         printerService = new PrinterService(printer);
    //         printerService.setContext(reactContext);
    //         promise.resolve(true);
    //     } catch (IOException e) {
    //         promise.reject(e);
    //     }
    // }

    // @ReactMethod
    // public void connectNetworkPrinter(String address, int port, Promise promise) {
    //     try {
    //         if (!"network".equals(config.getString("type"))) {
    //             promise.reject("config.type is not a network type");
    //         }
    //         Printer printer = new NetworkPrinter(address, port);
    //         printerService = new PrinterService(printer);
    //         printerService.setContext(reactContext);
    //         promise.resolve(true);
    //     } catch (IOException e) {
    //         promise.reject(e);
    //     }
    // }

    @ReactMethod
    public void disconnect(final String address, final Promise promise) {
        try {
            PrinterService printerService = printerServices.get(address);    
            if (Objects.isNull(printerService)) {
                throw new PrinterNotFoundException(ERROR_PRINTER_CONNECTION);
            }
            printerService.timerId = EscPosHelper.setTimeout(() -> {
                try {
                    printerService.close();
                    printerServices.remove(address);
                } catch (IOException e) {
                    promise.reject(e);
                }
             }, DISCONNECT_TIMEOUT);
            promise.resolve(true);
        } catch(PrinterNotFoundException e) {
            promise.reject(e);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    private void disconnectOnError(final String address) {
        try {
            PrinterService printerService = printerServices.get(address);

            if (Objects.nonNull(printerService)) {
                printerService.close();
                printerServices.remove(address);
            }
        } catch (IOException e) {
        } catch (Throwable e) {
        }
    }

    @SuppressWarnings({"MissingPermission"})
    @ReactMethod
    public void scanDevices() {
        scanManager.registerCallback(new ScanManager.OnBluetoothScanListener() {
            @Override
            public void deviceFound(BluetoothDevice bluetoothDevice) {
                WritableMap deviceInfoParams = Arguments.createMap();
                deviceInfoParams.putString("name", bluetoothDevice.getName());
                deviceInfoParams.putString("macAddress", bluetoothDevice.getAddress());

                // put deviceInfoParams into callbackParams
                WritableMap callbackParams = Arguments.createMap();
                callbackParams.putMap("deviceInfo", deviceInfoParams);
                callbackParams.putString("state", BluetoothEvent.DEVICE_FOUND.name());

                // emit callback to RN code
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("bluetoothDeviceFound", callbackParams);
            }
        });
        scanManager.startScan();
    }

    @ReactMethod
    public void stopScan() {
        scanManager.stopScan();
    }

    @ReactMethod
    public void initBluetoothConnectionListener() {
        // Add listener when bluetooth conencted
        reactContext.registerReceiver(bluetoothConnectionEventListener,
                new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

        // Add listener when bluetooth disconnected
        reactContext.registerReceiver(bluetoothConnectionEventListener,
                new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
    }

    /**
     * Bluetooth Connection Event Listener
     */
    @SuppressWarnings({"MissingPermission"})
    private BroadcastReceiver bluetoothConnectionEventListener = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String callbackAction = intent.getAction();
            BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            // if action or bluetooth data is null
            if (callbackAction == null || bluetoothDevice == null) {
                // do not proceed
                return;
            }

            // hold value for bluetooth event
            BluetoothEvent bluetoothEvent;

            switch (callbackAction) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    bluetoothEvent = BluetoothEvent.CONNECTED;
                    break;

                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    bluetoothEvent = BluetoothEvent.DISCONNECTED;
                    break;

                default:
                    bluetoothEvent = BluetoothEvent.NONE;
            }

            // bluetooth event must not be null
            if (bluetoothEvent != BluetoothEvent.NONE) {
                // extract bluetooth device info and put in deviceInfoParams
                WritableMap deviceInfoParams = Arguments.createMap();
                deviceInfoParams.putString("name", bluetoothDevice.getName());
                deviceInfoParams.putString("macAddress", bluetoothDevice.getAddress());

                // put deviceInfoParams into callbackParams
                WritableMap callbackParams = Arguments.createMap();
                callbackParams.putMap("deviceInfo", deviceInfoParams);
                callbackParams.putString("state", bluetoothEvent.name());

                // emit callback to RN code
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("bluetoothStateChanged", callbackParams);
            }
        }
    };
}
