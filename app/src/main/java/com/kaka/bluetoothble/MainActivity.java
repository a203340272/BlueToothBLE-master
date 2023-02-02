package com.kaka.bluetoothble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
//import androidx.appcompat.app.AppCompatActivity;
import android.support.v7.app.AppCompatActivity;
//import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.kaka.bluetoothble.adapter.BleAdapter;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.UUID;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
//import cn.bmob.v3.Bmob;
//import cn.bmob.v3.BmobObject;
//import cn.bmob.v3.exception.BmobException;
//import cn.bmob.v3.listener.SaveListener;

//mqtt************************
//import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {

    private static final String TAG ="ble_tag" ;
    ProgressBar pbSearchBle;
    ImageView ivSerBleStatus;
    TextView tvSerBleStatus;
    TextView tvSerBindStatus;
    ListView bleListView;             //滚动菜单
    private LinearLayout operaView;   //线性布局
    private Switch btnFire;           //second
    private Switch btnWater;          //third
    private Switch btnWindow1;         //forth  开关
    private Switch btnWindow2;         //拟为fifth
    private Button btnWrite;          //写数据按钮
    private Button btnRead;           //读数据三按钮  first
    private Button btnRead1;          //模拟数据一
    private Button btnRead2;          //模拟数据二
    private EditText etWriteContent;
    private TextView tvResponse;
    private List<BluetoothDevice> mDatas;    //设备数据初始化
    private List<Integer> mRssis;            //设备扫描距离初始化
    private BleAdapter mAdapter;                     //这个类映射了设备的蓝牙模块，蓝牙功能的使用将从它开始。
    private BluetoothAdapter mBluetoothAdapter;      //蓝牙是否打开
    private BluetoothManager mBluetoothManager;      //蓝牙管理
    private boolean isScaning=false;                 //扫描
    private boolean isConnecting=false;              //连接
    private BluetoothGatt mBluetoothGatt;            //BluetoothGatt对象 核心类
    //服务和特征值
    private UUID write_UUID_service;
    private UUID write_UUID_chara;
    private UUID read_UUID_service;        //读的service uuid

    public static final UUID read1_UUID_service = UUID.fromString("8edffff0-3d1b-9c37-4623-ad7265f14076");
    public static final UUID read1_UUID_chara = UUID.fromString("8edfffef-3d1b-9c37-4623-ad7265f14076");

    //private String read_UUID_chara;     //读的chara uuid
    private UUID read_UUID_chara;
    private UUID notify_UUID_service;
    private UUID notify_UUID_chara;
    private UUID indicate_UUID_service;
    private UUID indicate_UUID_chara;
    private String hex="7B46363941373237323532443741397D";

    //mqtt************************************************************************************
    //手机tcp://192.168.3.6:1883
    //模拟器tcp://10.0.2.2:1883
    private String host = "tcp://192.168.3.6:1883";
    private String userName = "admin";
    private String passWord = "admin";
    private String mqtt_id="APP";
    private int i = 1;
    private Handler handler;
    private MqttClient client;
    private String mqtt_sub_topic = "APP"; //为了保证你不受到别人的消息  哈哈  （自己）
    private String mqtt_pub_topic ="first";
    private MqttConnectOptions options;
    private ScheduledExecutorService scheduler;
    private String wenshidu = "NULL" ;   //存储温湿度数据
    //public static final String TAG = "MainActivity";
    private Handler mHandler;  //定时发送数据


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
       // Bmob.initialize(this, "162f8cc602e6ce8cd642136708b01bb1"); //初始化BmobSDK
        setContentView(R.layout.activity_search_device);
        initView();
        initData();
    //连接蓝牙的初始化
        //拿到BluetoothManager 再通过BluetoothManager.getAdapter()拿到BluetoothAdapter，判断蓝牙是否打开 没打开的话Intent隐式调用打开系统开启蓝牙界面
        mBluetoothManager= (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter=mBluetoothManager.getAdapter();
        if (mBluetoothAdapter==null||!mBluetoothAdapter.isEnabled()){
            Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,0);
        }
        //mqtt**************
        TextView text1 = findViewById(R.id.shoudaoxiaoxi);       //接收到的消息
        init();
        startReconnect();
        handler = new Handler() {

            @SuppressLint("SetTextIl8n")

            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1: //开机校验更新回传
                        break;
                    case 2: //反馈回转
                        break;
                    case 3: //MQTT收到消息回传
                        text1.setText(msg.obj.toString());
                        break;
                    case 30: //连接失败
                        Toast.makeText(MainActivity.this,"连接失败",Toast.LENGTH_SHORT).show();
                        break;
                    case 31: //连接成功
                        Toast.makeText(MainActivity.this,"连接成功",Toast.LENGTH_SHORT).show();
                        try {
                            client.subscribe(mqtt_sub_topic,2);//在连接上mqtt成功的位置确定要订阅的主题mqtt_sub_topic（为了测试，这个订阅的主题为另一个客户端发布的主题）
                            Log.d(TAG, "收到的信息");
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }

                      /*  if (wenshidu != null){ //如果获得了 读取数据三 温湿度数据，就发送

                          //publishmessageplus(mqtt_pub_topic,"第一个客户端发送的信息");//发布的方法
                          publishmessageplus(mqtt_pub_topic,"wenshidu");  //发布的方法
                            publishmessageplus("sixth",getRandomValue());     //模拟时和两个模拟一起发送
                            publishmessageplus("seventh",getRandomValue());
                          Log.d(TAG, "手机端发送的温湿度信息"+wenshidu);
                        }   */
                        btnFire = (android.widget.Switch) findViewById(R.id.btnFire);   //监听火警
                        btnFire.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @SuppressLint("HandlerLeak")
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if(isChecked) {
                                    //选中时 do some thing
                                    Toast.makeText(MainActivity.this, "火警：enabled",  Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "火警：1");
                                    publishmessageplus("second","1");  //发布的方法
                                } else {
                                    //非选中时 do some thing
                                    Toast.makeText(MainActivity.this, "火警：disabled", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "火警：0");
                                    publishmessageplus("second","0");
                                }
                            }
                        });
                        btnWater = (android.widget.Switch) findViewById(R.id.btnWater);   //监听水浸
                        btnWater.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @SuppressLint("HandlerLeak")
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if(isChecked) {
                                    //选中时 do some thing
                                    Toast.makeText(MainActivity.this, "水浸：enabled",  Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "水浸：1");
                                    publishmessageplus("third","1");  //发布的方法
                                } else {
                                    //非选中时 do some thing
                                    Toast.makeText(MainActivity.this, "水浸：disabled", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "水浸：0");
                                    publishmessageplus("third","0");
                                }
                            }
                        });
                        btnWindow1 = (android.widget.Switch) findViewById(R.id.btnWindow1);   //监听门窗1
                        btnWindow1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @SuppressLint("HandlerLeak")
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if(isChecked) {
                                    //选中时 do some thing
                                    Toast.makeText(MainActivity.this, "门窗一：enabled",  Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "门窗一：1");
                                    publishmessageplus("forth","1");  //发布的方法
                                } else {
                                    //非选中时 do some thing
                                    Toast.makeText(MainActivity.this, "门窗一：disabled", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "门窗一：0");
                                    publishmessageplus("forth","0");
                                }
                            }
                        });
                        btnWindow2 = (android.widget.Switch) findViewById(R.id.btnWindow2);   //监听门窗2
                        btnWindow2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @SuppressLint("HandlerLeak")
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if(isChecked) {
                                    //选中时 do some thing
                                    Toast.makeText(MainActivity.this, "门窗二：enabled",  Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "门窗二：1");
                                    publishmessageplus("fifth","1");  //发布的方法
                                } else {
                                    //非选中时 do some thing
                                    Toast.makeText(MainActivity.this, "门窗二：disabled", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "门窗二：0");
                                    publishmessageplus("fifth","0");
                                }
                            }
                        });
                        btnRead1=findViewById(R.id.btnRead1);          //模拟数据监听1
                        btnRead1.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(MainActivity.this, "模拟数据一："+getRandomValue(), Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "模拟数据一："+getRandomValue());
                                publishmessageplus("sixth",getRandomValue());
                            }
                        });
                        btnRead2=findViewById(R.id.btnRead2);          //模拟数据监听2
                        btnRead2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(MainActivity.this, "模拟数据二："+getRandomValue(), Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "模拟数据二："+getRandomValue());
                                publishmessageplus("seventh",getRandomValue());
                            }
                        });
                        break;
                    default:
                        break;
                }
            }
        };

    }

    private void initData() {
        mDatas=new ArrayList<>();   //设备数据
        mRssis=new ArrayList<>();   //设备扫描距离
        mAdapter=new BleAdapter(MainActivity.this,mDatas,mRssis);
        bleListView.setAdapter(mAdapter);    //下拉菜单
        mAdapter.notifyDataSetChanged();
    }

    private void initView(){
        pbSearchBle=findViewById(R.id.progress_ser_bluetooth);  //搜索按钮图
        ivSerBleStatus=findViewById(R.id.iv_ser_ble_status);    //图标
        tvSerBindStatus=findViewById(R.id.tv_ser_bind_status);  //已连接
        tvSerBleStatus=findViewById(R.id.tv_ser_ble_status);    //停止搜索
        //下半部分
        bleListView=findViewById(R.id.ble_list_view);      //搜索到的下拉菜单
        operaView=findViewById(R.id.opera_view);
        //连接之后的部分
        btnWrite=findViewById(R.id.btnWrite);        //写数据
        btnRead=findViewById(R.id.btnRead);          //读数据
        etWriteContent=findViewById(R.id.et_write);
        tvResponse=findViewById(R.id.tv_response);   //数据返回
        btnRead.setOnClickListener(new View.OnClickListener() {     //读数据调用函数readData
            @Override
            public void onClick(View v) {
                readData();

                mHandler = new Handler();      //每隔一段时间定时读数据，测试
                mHandler.post(new Runnable() {
                    @Override
                    public void run()
                    {
                        // TODO Auto-generated method stub
                        readData();
                        mHandler.postDelayed(this, 5000);
                    }
                });

            }
        });
        //点击监听，写数据
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //执行写入操作
                writeData();
            }
        });

         //点击监听，停止搜索
        ivSerBleStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScaning){
                    tvSerBindStatus.setText("停止搜索");
                    stopScanDevice();
                }else{
                    checkPermissions();
                }

            }
        });

       //滚顶菜单点击监听
        bleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isScaning){
                    stopScanDevice();
                }
                if (!isConnecting){
                    isConnecting=true;
                    BluetoothDevice bluetoothDevice= mDatas.get(position);
                    //连接设备 6.0及以上连接设备的方法是1，以下是2 TRANSPORT_LE是设置传输层模式 不传默认TRANSPORT_AUTO
                    //bluetoothDevice.connectGatt（）返回BluetoothGatt，单独声明成全局变量来使用，设备的读、写和订阅等操作都需要用
                    tvSerBindStatus.setText("连接中");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mBluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this,
                                true, gattCallback, TRANSPORT_LE);
                    } else {
                        mBluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this,
                                true, gattCallback);
                    }
                }

            }
        });


    }

    private void readData() {  //读数据 read_UUID_service:8edffff0-3d1b-9c37-4623-ad7265f14076 read_UUID_chara:
                         //UUID.fromString("8edfffef-3d1b-9c37-4623-ad7265f14076")
        BluetoothGattCharacteristic characteristic=mBluetoothGatt.getService(read1_UUID_service)
                .getCharacteristic(read1_UUID_chara);
        mBluetoothGatt.readCharacteristic(characteristic);
        Log.d(TAG, "readData():读取数据：" +mBluetoothGatt.readCharacteristic(characteristic)+"读取数据："+read1_UUID_service+"读取数据："+read1_UUID_chara);
    }
    /**
     * 开始扫描 10秒后自动停止
     * */
    private void scanDevice(){
        tvSerBindStatus.setText("正在搜索");
        isScaning=true;
        pbSearchBle.setVisibility(View.VISIBLE);
        mBluetoothAdapter.startLeScan(scanCallback);   //开始扫描 scanCallback对象在后便写了
        new Handler().postDelayed(new Runnable() {    //延迟
            @Override
            public void run() {
                //结束扫描
                mBluetoothAdapter.stopLeScan(scanCallback);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isScaning=false;
                        pbSearchBle.setVisibility(View.GONE);
                        tvSerBindStatus.setText("搜索已结束");
                    }
                });
            }
        },10000);
    }

    /**
     * 停止扫描
     * */
    private void stopScanDevice(){
        isScaning=false;
        pbSearchBle.setVisibility(View.GONE);
        mBluetoothAdapter.stopLeScan(scanCallback);
    }

//device是设备对象，rssi扫描到的设备强度，scanRecord是扫面记录
    BluetoothAdapter.LeScanCallback scanCallback=new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.e(TAG, "run: scanning...");
            if (!mDatas.contains(device)){
                mDatas.add(device);
                mRssis.add(rssi);
                mAdapter.notifyDataSetChanged();
            }

        }
    };

    private BluetoothGattCallback gattCallback=new BluetoothGattCallback() {
        /**
         * 断开或连接 状态发生变化时调用 （重复连接可能报133连接失败，调用mBluetoothGatt.close()解决，回调里的方法不要做耗时操作，不要在回调方法里更新UI，可能会阻塞线程。
         * */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.e(TAG,"onConnectionStateChange()");
            if (status==BluetoothGatt.GATT_SUCCESS){
                //连接成功
                if (newState== BluetoothGatt.STATE_CONNECTED){
                    Log.e(TAG,"连接成功");
                    //发现服务
                    gatt.discoverServices();
                }
            }else{
                //连接失败
                Log.e(TAG,"失败=="+status);
                mBluetoothGatt.close();
                isConnecting=false;
            }
        }
        /**
         * 发现设备（真正建立连接）发现服务成功后回调onServicesDiscovered。服务的获取，特征的读写，描述符的读写，设置特征通知等都在该方法里写
         * */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            //直到这里才是真正建立了可通信的连接
            isConnecting=false;
            Log.e(TAG,"onServicesDiscovered()---建立连接");
            //获取初始化服务和特征值  后边的initServiceAndChara通过Android拿得到对应UUID.
            initServiceAndChara();
            //订阅通知  enable弹出一个系统的是否打开/关闭蓝牙的对话框   getService获取Gatt中指定UUID的service getCharacteristic获取此服务提供的特征列表之外具有给定UUID的特征。
            mBluetoothGatt.setCharacteristicNotification(mBluetoothGatt
                    .getService(notify_UUID_service).getCharacteristic(notify_UUID_chara),true);
            runOnUiThread(new Runnable() {      //UI界面显示
                @Override
                public void run() {
                    bleListView.setVisibility(View.GONE);  //控件不可见
                    operaView.setVisibility(View.VISIBLE); //控件可见
                    tvSerBindStatus.setText("已连接");
                }
            });
        }
        /**
         * 读操作的回调
         * */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.e(TAG," 读操作的回调onCharacteristicRead()");
            //先使用readCharacteristic(characteristic)，然后在OncharacteristicRead回调函数中使用characteristic.getValue() 来读取。
            //Log.e(TAG," 读操作的回调onCharacteristicChanged()"+characteristic.getValue());
            Log.e(TAG," 温湿度数据："+bytes2hex(characteristic.getValue()));
            wenshidu= bytes2hex(characteristic.getValue());    //记下来数据
            //如果获得了温湿度数据，就发送
            if(wenshidu != null) {
                //publishmessageplus(mqtt_pub_topic,"第一个客户端发送的信息");//发布的方法
                publishmessageplus(mqtt_pub_topic, wenshidu);//发布的方法
                publishmessageplus("sixth",getRandomValue());     //模拟时和两个模拟一起发送
                publishmessageplus("seventh",getRandomValue());
                Log.d(TAG, "手机端发送的温湿度信息" + wenshidu);
            }
            final byte[] data=characteristic.getValue();   //读数据
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addText(tvResponse,bytes2hex(data));
                }
            });
        }
         /**
       * 写操作的回调
         * */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            Log.e(TAG,"onCharacteristicWrite()  status="+status+",value="+HexUtil.encodeHexStr(characteristic.getValue()));
        }
        /**
         * 接收到硬件返回的数据
         * */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.e(TAG,"onCharacteristicChanged()"+characteristic.getValue());
            final byte[] data=characteristic.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addText(tvResponse,bytes2hex(data));
                }
            });

        }
    };
    /**
     *  检查权限     Android6.0系统以上开启蓝牙还需要定位权限 使用了RxPerssion动态库动态申请·
     */
    private void checkPermissions() {
        RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
        rxPermissions.request(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new io.reactivex.functions.Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            // 用户已经同意该权限
                            scanDevice();
                        } else {
                            // 用户拒绝了该权限，并且选中『不再询问』
                            ToastUtils.showLong("用户开启权限后才能使用");
                        }
                    }
                });
    }

    /**
     *  initServiceAndChara 通过Android拿得到对应UUID
     */
    private void initServiceAndChara(){
        List<BluetoothGattService> bluetoothGattServices= mBluetoothGatt.getServices();
        for (BluetoothGattService bluetoothGattService:bluetoothGattServices){
            List<BluetoothGattCharacteristic> characteristics=bluetoothGattService.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic:characteristics){
                int charaProp = characteristic.getProperties();
                //BluetoothGattCharacteristic.PROPERTY_READ：读取数据 PROPERTY_WRITE和PROPERTY_WRITE_NO_RESPONSE：写
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    read_UUID_chara=characteristic.getUuid();
                    read_UUID_service=bluetoothGattService.getUuid();
                    read_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"read_chara="+read_UUID_chara+"----read_service="+read_UUID_service);
                   // readData();
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    write_UUID_chara=characteristic.getUuid();
                    write_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"write_chara="+write_UUID_chara+"----write_service="+write_UUID_service);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                    write_UUID_chara=characteristic.getUuid();
                    write_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"write_chara="+write_UUID_chara+"----write_service="+write_UUID_service);
                }
                //PROPERTY_NOTIFY和PROPERTY_INDICATE：都是订阅的方法 INDICATE一定接收到订阅回调，接收重要的，不能太频繁；而PROPERTY_NOTIFY不一定能接收到回调，可以频繁接收，使用得比较多的订阅方式。
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    notify_UUID_chara=characteristic.getUuid();
                    notify_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"notify_chara="+notify_UUID_chara+"----notify_service="+notify_UUID_service);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                    indicate_UUID_chara=characteristic.getUuid();
                    indicate_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"indicate_chara="+indicate_UUID_chara+"----indicate_service="+indicate_UUID_service);
                }
            }
        }
    }

    private void addText(TextView textView, String content) {
        textView.append(content);
        textView.append("\n");
        int offset = textView.getLineCount() * textView.getLineHeight();
        if (offset > textView.getHeight()) {
            textView.scrollTo(0, offset - textView.getHeight());
        }
    }

    private void writeData(){
        BluetoothGattService service=mBluetoothGatt.getService(write_UUID_service);
        BluetoothGattCharacteristic charaWrite=service.getCharacteristic(write_UUID_chara);
        byte[] data;
        String content=etWriteContent.getText().toString();
        if (!TextUtils.isEmpty(content)){
            data=HexUtil.hexStringToBytes(content);
        }else{
            data=HexUtil.hexStringToBytes(hex);
        }
        if (data.length>20){//数据大于个字节 分批次写入
            Log.e(TAG, "writeData: length="+data.length);
            int num=0;
            if (data.length%20!=0){
                num=data.length/20+1;
            }else{
                num=data.length/20;
            }
            for (int i=0;i<num;i++){
                byte[] tempArr;
                if (i==num-1){
                    tempArr=new byte[data.length-i*20];
                    System.arraycopy(data,i*20,tempArr,0,data.length-i*20);
                }else{
                    tempArr=new byte[20];
                    System.arraycopy(data,i*20,tempArr,0,20);
                }
                charaWrite.setValue(tempArr);
                mBluetoothGatt.writeCharacteristic(charaWrite);
            }
        }else{
            charaWrite.setValue(data);
            mBluetoothGatt.writeCharacteristic(charaWrite);
        }
    }

    private static final String HEX = "0123456789abcdef";
    public static String bytes2hex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
        {
            // 取出这个字节的高4位，然后与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt((b >> 4) & 0x0f));
            // 取出这个字节的低位，与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt(b & 0x0f));
        }
        return sb.toString();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothGatt.disconnect();
    }
//***********************************************************************************************************************
//mqtt******************************************************************************************
//***********************************************************************************************************************
    private void init() {
        try {
            //host为主机名，test为clientid即连接MQTT的客户端ID，一般以客户端唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            client = new MqttClient(host, mqtt_id,
                    new MemoryPersistence());
            //MQTT的连接设置
            options = new MqttConnectOptions();
            //设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(true);
            //设置连接的用户名
            options.setUserName(userName);
            //设置连接的密码
            options.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(20);
            //设置回调
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    //连接丢失后，一般在这里面进行重连
                    System.out.println("connectionLost----------");
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //publish后会执行到这里
                    System.out.println("deliveryComplete---------"
                            + token.isComplete());
                }
                @Override
                public void messageArrived(String topicName, MqttMessage message)  //订阅信息
                        throws Exception {
                    //subscribe后得到的消息会执行到这里面
                    System.out.println("messageArrived----------");
                    Message msg = new Message();
                    msg.what = 3;
                    msg.obj = topicName + "---" + message.toString();
                    handler.sendMessage(msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void Mqtt_connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!(client.isConnected())){
                        client.connect(options);
                        Message msg = new Message();
                        msg.what=31;
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.what = 30;
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }

    private void startReconnect() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
          @Override
            public void run() {
                if (!client.isConnected()) {
                    Mqtt_connect();
                }
            }
        }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    private void publishmessageplus(String topic,String message2)  //发布
    {
        if (client == null || !client.isConnected()) {
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setPayload(message2.getBytes());
        try {
            client.publish(topic,message);   //发布
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    /*产生10位16进制的数*/
    public static String getRandomValue() {
        String str = "";
        for (int i = 0; i < 10; i++) {
            char temp = 0;
            int key = (int) (Math.random() * 2);
            switch (key) {
                case 0:
                    temp = (char) (Math.random() * 10 + 48);//产生随机数字
                    break;
                case 1:
                    temp = (char) (Math.random()*6 + 'a');//产生a-f
                    break;
                default:
                    break;
            }
            str = str + temp;
        }
        return str;
    }

}