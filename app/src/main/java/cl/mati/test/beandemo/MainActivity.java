package cl.mati.test.beandemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.DeviceInfo;
import com.punchthrough.bean.sdk.message.ScratchBank;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private BluetoothAdapter mBluetoothAdapter;
    boolean bluetoothIsEnabled;
    boolean isBeanConnected;
    final List<Bean> beans = new ArrayList<>();
    private Bean connectedBean;
    final Context context = this;
    private ArrayAdapter<String> adapter;
    private ListView beansListView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_SHORT).show();
        } else
        {
            Toast.makeText(this, "BLE is supported", Toast.LENGTH_SHORT).show();
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(this, "error: bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();

            // mBluetoothAdapter.isEnabled() mejor hacerlo cada vez que se aprete porque el loco
            // puede activarlo en medio del uso de la app
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        beansListView = (ListView) findViewById(R.id.beansListView);
        ArrayList<String> beanList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,android.R.id.text1,beanList);
        beansListView.setAdapter(adapter);
        beansListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String  itemValue    = (String) beansListView.getItemAtPosition(position);
                Log.i("MainActivity","onItemClick, pos:" + Integer.toString(position) +", item:" + itemValue);

                cancelScann(false);

                // Assume we have a reference to the 'beans' ArrayList from above.
                final Bean beanElement = beans.get(0);

                beanElementClicked(beanElement);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*                                  */
    /*           onClick's              */
    /*                                  */

    // onCLick - info pressed
    public void infoPressed(View view)
    {
        if (mBluetoothAdapter.isEnabled())
        {
            adapter.clear();
            adapter.notifyDataSetChanged();
            BeanDiscoveryListener listener = new BeanDiscoveryListener()
            {
                @Override
                public void onBeanDiscovered(Bean bean, int rssi)
                {
                    adapter.add("name: " + bean.getDevice().getName() + "    adrr: " + bean.getDevice().getAddress());
                    //adapter.add("rssi:" + Integer.toString(rssi) + " name:" + bean.getDevice().getName() + " (" + bean.getDevice().getAddress() + ")");
                    adapter.notifyDataSetChanged();
                    beans.add(bean);
                }

                @Override
                public void onDiscoveryComplete()
                {
                    Toast.makeText(context, "Bean listener finished", Toast.LENGTH_SHORT).show();
                    Log.i("MainActivity", "onDiscoveryComplete");
                    TextView info = (TextView) findViewById(R.id.infoTextView);
                    info.setText("Scanning complete.");
                    FloatingActionButton newmail = (FloatingActionButton) findViewById(R.id.newmail);
                    newmail.setVisibility(View.GONE);
                }
            };

            BeanManager.getInstance().startDiscovery(listener);

            TextView info = (TextView) findViewById(R.id.infoTextView);
            info.setText("Scanning...");
            FloatingActionButton newmail = (FloatingActionButton) findViewById(R.id.newmail);
            newmail.setVisibility(View.VISIBLE);

            Toast.makeText(this, "Press new mail to cancel ->", Toast.LENGTH_SHORT).show();
            Snackbar.make(view, "Bean listener started", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
        else //Bluetooth disabled
        {
            disabledBluetoothAction();
        }
    }

    public void newMail(View view) //cancelButton
    {
        if(connectedBean != null)
        {
            if(connectedBean.isConnected())
            {
                connectedBean.disconnect();
                connectedBean = null;
                TextView info = (TextView) findViewById(R.id.infoTextView);
                info.setText("Bean Disconnected.");
                FloatingActionButton newmail = (FloatingActionButton) findViewById(R.id.newmail);
                newmail.setVisibility(View.GONE);
            }
            else
            {
                Log.e("MainActivity","newMail, ERROR: connectedBean != null && connectedBean disconnected, no valid states");
            }
        }
        else
        {
            cancelScann(true);
        }
    }

    protected void cancelScann(boolean textFlag)
    {
        BeanManager.getInstance().cancelDiscovery();
        TextView info = (TextView) findViewById(R.id.infoTextView);
        String infoText = (textFlag)? "Scanning Canceled.":"Connecting...";
        info.setText(infoText);
        FloatingActionButton newmail = (FloatingActionButton) findViewById(R.id.newmail);
        newmail.setVisibility(View.GONE);
    }

    public void disabledBluetoothAction()
    {
        Toast.makeText(this,"Enable bluetooth communication, please :)",Toast.LENGTH_SHORT).show();
        //show notification for accesing bluetooth settings
    }

    // customOnClick - beanListView element
    protected void beanElementClicked(final Bean bean)
    {
        BeanListener beanListener = new BeanListener()
        {
            @Override
            public void onConnected()
            {
                connectedBean = bean;

                TextView info = (TextView) findViewById(R.id.infoTextView);
                info.setText(bean.getDevice().getName() + " Connected.");
                FloatingActionButton newmail = (FloatingActionButton) findViewById(R.id.newmail);
                newmail.setVisibility(View.VISIBLE);

                Log.i("MainActivity","connected to '" + bean.getDevice().getName());
                bean.readTemperature(new Callback<Integer>()
                {
                    @Override
                    public void onResult(Integer temp)
                    {
                        Toast.makeText(context, bean.getDevice().getName() + " temp = " + Integer.toString(temp) + "°C", Toast.LENGTH_SHORT).show();
                        Log.i("MainActivity","onConnected," + bean.getDevice().getName() + "temp = " + Integer.toString(temp) + "°C");
                    }
                });

            }

            @Override
            public void onConnectionFailed()
            {
                Log.i("MainActivity","BeanListener.onConnectionFailed");
            }

            @Override
            public void onDisconnected()
            {
                Log.i("MainActivity","BeanListener.onDisconnected");
                connectedBean = null;
                TextView info = (TextView) findViewById(R.id.infoTextView);
                info.setText("Bean Disconnected.");
                FloatingActionButton newmail = (FloatingActionButton) findViewById(R.id.newmail);
                newmail.setVisibility(View.GONE);
            }

            @Override
            public void onSerialMessageReceived(byte[] data)
            {
                Log.i("MainActivity","BeanListener.onSerialMessageReceived");
                Integer dataLength = data.length;
                if (dataLength != 1)
                {
                    // ¿Hay que revisar cuando el length puede ser 0?
                    Log.i("MainActivity", "BeanListener message length: " + Integer.toString(dataLength));
                    char[] dataChar = new char[dataLength];
                    //byte b;
                    for (int i = 0; i < dataLength; i++)
                    {
                        //Log.i("MainActivity","BeanListener msg: " + Byte.toString(b) + "(end)");
                        dataChar[i] = (char) data[i];
                    }
                    String msg = new String(dataChar);
                    Log.i("MainActivity", "BeanListener message content: " + msg);

                    TextView info = (TextView) findViewById(R.id.infoTextView);
                    Snackbar.make(info, "Serial msg: " + msg, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                }
                else
                {
                    //Pensar que hacer con lo CR LF - \r\n
                }
            }

            @Override
            public void onScratchValueChanged(ScratchBank bank, byte[] value)
            {
                Log.i("MainActivity","BeanListener.onScratchValueChanged");
            }

            @Override
            public void onError(BeanError error)
            {
                Log.i("MainActivity","BeanListener.onError");
            }

            @Override
            public void onReadRemoteRssi(int rssi)
            {
                Log.i("MainActivity","BeanListener.onReadRemoteRssi");
            }
        };

        // Assuming you are in an Activity, use 'this' for the context
        bean.connect(context, beanListener);

    }
}
