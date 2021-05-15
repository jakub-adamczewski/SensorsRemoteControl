package pl.jkapps.sensorsremotecontrol

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

//MainActivity to główny ekran aplikacji, dziedziczymy po klasie AppCompatActivity(), dzięki czemu
//mamy system Android wie, ze klasa MainActivity jest Activity (czyli widokiem) i mamy dostęp
//do jej różnych funkcjonalności
class MainActivity : AppCompatActivity() {

    companion object {
//        TAG służy tylko do testowania aplikacji
        private val TAG = "MainActivity"
//        UUID (universally unique identifier) do tworzenia Socketu Bluetooth
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

//    Kod obsługujący odpowiedź z systemowego widoku do uruchamiania Bluetooth
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    Toast.makeText(this, "Bluetooth ON", Toast.LENGTH_SHORT).show()
                }
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(this, "Bluetooth OFF", Toast.LENGTH_SHORT).show()
                }
            }
        }

//    obiekt obsługujący tworzenie połączenia z modułem HC-05, lub innym dowolnym urządzeniem Bluetooth
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//    zmienna do której zapiszemy moduł HC-05 z którym będziemy się komunikować
    private lateinit var btDevice: BluetoothDevice
//    obiekt służący do wysyłania danych do podłączonego urządzenia
    private lateinit var btSocket: BluetoothSocket

//    funkcja onCreate wywołuje się przy tworzeniu widoku
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//    podpięcie pliku widoku (*.xml) do pliku z logiką (*.kt)
        setContentView(R.layout.activity_main)

//    przygotowanie działania aplikacji
        setButtonsListeners()
        turnOnBluetooth()
    }

//    przypisanie akcji do przycisków
    private fun setButtonsListeners() {
//    ustawenie wywołania funkcji connect() po kliknieciu przycisku connect
        connect_btn.setOnClickListener {
            connect()
        }
//    ustawenie oddzielnej funkcjonalności w momencie kiedy użytkownik dotknie i puści przycisk
//    na dotknięcie wysyłamy do robota sygnał określający ruch do przodu
//    a podczas puszczenia przycisku wysyłamy sygnał określający zatrzymanie
    forward_tv.setOnTouchListener { _, event ->
            return@setOnTouchListener when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "forward down")
                    send(Direction.FORWARD)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "forward up")
                    send(Direction.STOP)
                    true
                }
                else -> false
            }
        }
//    ustawenie oddzielnej funkcjonalności w momencie kiedy użytkownik dotknie i puści przycisk
//    na dotknięcie wysyłamy do robota sygnał określający ruch do tyłu
//    a podczas puszczenia przycisku wysyłamy sygnał określający zatrzymanie
        backward_tv.setOnTouchListener { _, event ->
            return@setOnTouchListener when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "backward down")
                    send(Direction.BACKWARD)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "backward up")
                    send(Direction.STOP)
                    true
                }
                else -> false
            }
        }
//    ustawenie oddzielnej funkcjonalności w momencie kiedy użytkownik dotknie i puści przycisk
//    na dotknięcie wysyłamy do robota sygnał określający skręt w lewo
//    a podczas puszczenia przycisku wysyłamy sygnał określający zatrzymanie
        left_tv.setOnTouchListener { _, event ->
            return@setOnTouchListener when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "left down")
                    send(Direction.LEFT)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "left up")
                    send(Direction.STOP)
                    true
                }
                else -> false
            }
        }
//    ustawenie oddzielnej funkcjonalności w momencie kiedy użytkownik dotknie i puści przycisk
//    na dotknięcie wysyłamy do robota sygnał określający skręt w prawo
//    a podczas puszczenia przycisku wysyłamy sygnał określający zatrzymanie
        right_tv.setOnTouchListener { _, event ->
            return@setOnTouchListener when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "right down")
                    send(Direction.RIGHT)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "right up")
                    send(Direction.STOP)
                    true
                }
                else -> false
            }
        }
    }

//funkcja uruchamiająca widok z prośbą o włączenie Bluetooth w smartfonie
    private fun turnOnBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            resultLauncher.launch(enableBtIntent)
        }
    }

//    funkcja znajdująca spaowane urządzenia bluetooth (aby połączyc się z modułem HC-05 trzeba się
//    z nim najpierw sparować w ustawieniach Bluetooth
    private fun findDevices(): Set<BluetoothDevice>? {
//    pobranie sparowanych urządzeń z adaptera bluetooth
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            Log.d("Test__", "$deviceName")
        }
        return pairedDevices
    }

//połączenie z modułem HC-05
    private fun connect() {
//    wyszukanie modułu HC-05 wśród sparowanych urządzeń
        val potentialDevice = findDevices()?.firstOrNull { it.name == "HC-05" }
        if (potentialDevice != null) {
//    przeprowadzanie łączenia z modułem
            btDevice = bluetoothAdapter.getRemoteDevice(potentialDevice.address)
            btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID)
            btSocket.connect()
            makeToast(
                if (btSocket.isConnected) {
                    "Connected with HC-05"
                } else {
                    "Could not connect with HC-05"
                }
            )
        } else {
//    wyświetlanie informacji o konieczności sparowania
            makeToast("Not paired with HC-05")
        }
    }

//    funkcja ułatwiajaca wyświetlanie Toast'ów (dymków z wiadomościami)
    private fun makeToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

// funkcja używana do wyświetlania określonych sygnałów do modułu
    private fun send(direction: Direction){
        if(btSocket == null){
            makeToast("Not connected to HC-05")
        }else {
            btSocket.outputStream.write(direction.code)
        }
    }

//    klasa enum zawierająca kody oznaczające określone akcje (tak aby kody były zdefiniowane w jednym miejscu)
//    by uniknąć ewentualnych pomyłek
    enum class Direction(val code: Int){
        FORWARD(11),
        BACKWARD(12),
        LEFT(13),
        RIGHT(14),
        STOP(15)
    }
}