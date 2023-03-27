package com.example.administrator.osmapitest.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.administrator.osmapitest.R;
import com.example.administrator.osmapitest.data.ClientPos;
import com.example.administrator.osmapitest.indoormap.DrawIndoorMap;
import com.example.administrator.osmapitest.indoormap.GetIndoorMapService;
import com.example.administrator.osmapitest.indoormap.IndoorMap;
import com.example.administrator.osmapitest.location.indoorloc.inertial.InertialLocateService;
import com.example.administrator.osmapitest.location.indoorloc.nfc.NfcBaseActivity;
import com.example.administrator.osmapitest.location.indoorloc.wifi.WifiLocateService;
import com.example.administrator.osmapitest.location.outdoorloc.OutdoorLocListener;
import com.example.administrator.osmapitest.navigation.DrawNavRoute;
import com.example.administrator.osmapitest.navigation.NavigationService;
import com.example.administrator.osmapitest.navigation.NavigationThread;
import com.example.administrator.osmapitest.shared.NowClientPos;
import com.example.administrator.osmapitest.shared.Status;
import com.example.administrator.osmapitest.trans.PosDataTransService;
import com.example.administrator.osmapitest.util.AlertDialogFactory;
import com.example.administrator.osmapitest.util.AreaJudgeReceiver;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The main activity of indoor/outdoor seamless positioning and navigation system
 */
@SuppressLint("MissingPermission")
public class MainActivity extends NfcBaseActivity implements View.OnClickListener,
        ViewTreeObserver.OnGlobalLayoutListener {

    private MapView mMapView;
    private IMapController mController;
    private DirectedLocationOverlay mDLOverlay;
    private DrawIndoorMap mDrawIndoorMap;

    private LocationManager mLocationManager;
    private OutdoorLocListener mLocationListener;

    private DrawNavRoute mDrawNavRoute;
    private NavigationThread mNavigationThread;

    private UIHandler uiHandler;

    private DataReceiver dataReceiver;
    private StatusChangeReceiver statusChangeReceiver;
    private AreaJudgeReceiver areaJudgeReceiver;

    private Intent posDataTransIntent;
    private Intent inertialLocIntent;
    private Intent wifiLocateIntent;

    private EditText seekEdit;
    private Button btnSeek;

    private static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();   // Hide the title bar
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);    // Hide the status bar
        setContentView(R.layout.activity_main);
        getPermissions();   // Aquisição de permissão dinâmica
        uiHandler = new UIHandler();
        mDrawNavRoute = new DrawNavRoute();
        initMap();  // Inicializar o mapa
        initWidget();   // Inicializar controles
        mNavigationThread = new NavigationThread(mDrawNavRoute, getApplicationContext(),
                mMapView, uiHandler);
        posDataTransIntent =
                new Intent(MainActivity.this, PosDataTransService.class);
        wifiLocateIntent =
                new Intent(MainActivity.this, WifiLocateService.class);
        inertialLocIntent =
                new Intent(MainActivity.this, InertialLocateService.class);

        initReceiver(); // Initialize broad receiver
        initOutdoorLocation(); // Initialize outdoor positioning module
    }

    /**
     * Initialize map and its widget
     */
    public void initMap() {
        mMapView = findViewById(R.id.my_osm_map_view);
        mDrawIndoorMap = new DrawIndoorMap(mMapView, uiHandler);
        mController = mMapView.getController();
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);
        // ícone de localização do usuário
        mDLOverlay = new DirectedLocationOverlay(getApplication());
        mDLOverlay.setEnabled(true);
        // escala
        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mScaleBarOverlay.enableScaleBar();
        mScaleBarOverlay.setAlignBottom(true);
        mScaleBarOverlay.setAlignRight(true);
        mScaleBarOverlay.setLineWidth(1 * (getResources().getDisplayMetrics()).density);
        mScaleBarOverlay.setMaxLength(1f);
        mMapView.getOverlays().add(mScaleBarOverlay);
        // Inicializar a localização atual
        GeoPoint center = new GeoPoint(31.866942, 117.282699);
        mController.setCenter(center);
        mController.setZoom(18);
    }

    /**
     * Initialize broad receiver
     */
    private void initReceiver() {
        dataReceiver = new DataReceiver();
        IntentFilter dataIntentFilter = new IntentFilter();
        dataIntentFilter.addAction("locate");
        dataIntentFilter.addAction("indoor_map");
        dataIntentFilter.addAction("no_map");
        dataIntentFilter.addAction("navigate");
        dataIntentFilter.addAction("no_nav_info");
        dataIntentFilter.addAction("stop_nav");
        registerReceiver(dataReceiver, dataIntentFilter);

        statusChangeReceiver = new StatusChangeReceiver();
        IntentFilter statusIntentFilter = new IntentFilter();
        statusIntentFilter.addAction("outdoor_to_indoor");
        statusIntentFilter.addAction("indoor_to_outdoor");
        statusIntentFilter.addAction("init_indoor");
        statusIntentFilter.addAction("init_outdoor");
        registerReceiver(statusChangeReceiver, statusIntentFilter);

        areaJudgeReceiver = new AreaJudgeReceiver();
        IntentFilter areaIntentFilter = new IntentFilter();
        areaIntentFilter.addAction("locate");
        registerReceiver(areaJudgeReceiver, areaIntentFilter);
    }

    /**
     * Initialize widget
     */
    private void initWidget() {
        Button Building_high = findViewById(R.id.building_high);
        Button wifiPos = findViewById(R.id.wifi_locate);
        Button route_line = findViewById(R.id.route_line);
        seekEdit = findViewById(R.id.dest_edit_text);
        btnSeek = findViewById(R.id.seek_button);
        btnSeek.setEnabled(false);
        btnSeek.getBackground().mutate().setAlpha(153);
        seekEdit.getBackground().mutate().setAlpha(153);
        seekEdit.getViewTreeObserver().addOnGlobalLayoutListener(this);
        route_line.setOnClickListener(this);
        wifiPos.setOnClickListener(this);
        Building_high.setOnClickListener(this);
        btnSeek.setOnClickListener(this);
        FloatingActionButton locateFAButton = findViewById(R.id.locate_fab);
        locateFAButton.setOnClickListener(this);
    }

    /**
     * Initialize outdoor positioning module
     */
    private void initOutdoorLocation() {
        mLocationListener = new OutdoorLocListener(getApplicationContext());
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        Objects.requireNonNull(mLocationManager).requestLocationUpdates(LocationManager.GPS_PROVIDER,
                500, 0, mLocationListener);
        Objects.requireNonNull(mLocationManager).requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                500, 0, mLocationListener);
    }

    private boolean isFirstLocate = true;
    private boolean isInitIndoor = false;

    /**
     * The broadcast receiver receive location result
     */
    class DataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case "locate":
                    ClientPos ClientPos = (ClientPos) intent.getSerializableExtra("pos_data");
                    NowClientPos.setPosPara(ClientPos);
                    GeoPoint geoPoint = new GeoPoint(ClientPos.getLatitude(), ClientPos.getLongitude());
                    mDLOverlay.setLocation(geoPoint);
                    if (isFirstLocate) {
                        mController.animateTo(geoPoint);
                        isFirstLocate = false;
                        mMapView.getOverlays().add(mDLOverlay);
                    }
                    updateLocOverlay();
                    if (isInitIndoor) {   // Se a localização inícial for indoor, solicite o mapa através do resultado do posicionamento wi-fi
                        isInitIndoor = false;
                        Intent getIndoorMapIntent = getIndoorMapIntent(NowClientPos.getNowFloor(),
                                NowClientPos.getNowLongitude(), NowClientPos.getNowLatitude());
                        startService(getIndoorMapIntent);
                        // Open inertial positioning module
                        inertialLocIntent.putExtra("init_pos", ClientPos);
                        startService(inertialLocIntent);
                    }
                    break;
                case "indoor_map":
                    IndoorMap indoorMap = (IndoorMap) intent.getSerializableExtra("indoor_map");
                    mDrawIndoorMap.drawIndoorMap(indoorMap);
                    // After loading the indoor map,
                    // the current location needs to be updated to prevent the indoor map from blocking the user icon
                    updateLocOverlay();
                    mMapView.invalidate();
                    break;
                case "no_map":  // There is no indoor map for current area
                    AlertDialogFactory.getNoMapDialog(getApplicationContext());
                    break;
                case "navigate":    // Navigation
                    mDrawNavRoute.removeNavRoute(mMapView, uiHandler);
                    mNavigationThread.setIntent(intent);
                    new Thread(mNavigationThread).start();
                    break;
                case "stop_nav":    // Stop navigation
                    mDrawNavRoute.removeNavRoute(mMapView, uiHandler);
                    break;
                case "no_nav_info": // There is no area in the database
                    new AlertDialog.Builder(context)
                            .setTitle("系统提示")
                            .setMessage("目标区域不存在，请重新确认").setCancelable(false)
                            .setNegativeButton("确定", null).show();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Broadcast receiver for receiving position status
     */
    class StatusChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                /*
                  As operações que precisam ser realizadas incluem o carregamento do mapa interno
                  Feche o posicionamento externo, abra o módulo de posicionamento interno
                 */
                case "outdoor_to_indoor":
                    Toast.makeText(context, "outdoor to indoor", Toast.LENGTH_SHORT).show();
                    updateLocOverlay();
                    // Entrando no comodo apartir de uma area externa, os dados do mapa solicitados devem ser a primeira camada
                    Intent getIndoorMapIntent = getIndoorMapIntent(1,
                            NowClientPos.getNowLongitude(), NowClientPos.getNowLatitude());
                    startService(getIndoorMapIntent);
                    mLocationManager.removeUpdates(mLocationListener);
                    startIndoorLoc();
                    break;
                /*
                  A operação que precisa ser feita é remover o mapa interno
                  Feche o posicionamento interno, abra o módulo de posicionamento externo
                 */
                case "indoor_to_outdoor":
                    Toast.makeText(context, "indoor to outdoor", Toast.LENGTH_SHORT).show();
                    stopIndoorLoc();
                    initOutdoorLocation();
                    mDrawIndoorMap.removeIndoorMap();
                    break;
                /*
                  Dentro de casa desde o início
                  As operações que precisam ser executadas incluem desabilitar o posicionamento GPS, habilitar o posicionamento interno e solicitar mapas internos do servidor.
                 */
                case "init_indoor":
                    unregisterReceiver(areaJudgeReceiver);  // O inicial da área foi concluído, remova o ouvinte da transmissão
                    Status.setIsIndoor(true);
                    mLocationManager.removeUpdates(mLocationListener);  // Fecha o módulo de posicionamento externo
                    isCanReadNfc = true;    // Ligua o módulo de leitura de NFC
                    startService(wifiLocateIntent); // Liga o módulo de posicionamento Wifi
                    isInitIndoor = true;
                    Toast.makeText(context, "start indoor only", Toast.LENGTH_SHORT).show();
                    uiHandler.postDelayed(new Runnable() {  // Início atrasado do módulo de transmissão de dados
                        public void run() {
                            startService(posDataTransIntent);
                        }
                    }, 2000);
                    break;
                case "init_outdoor":
                    unregisterReceiver(areaJudgeReceiver);  // O inicial da área foi concluído, remova o ouvinte da transmissão
                    Status.setIsIndoor(false);
                    startService(posDataTransIntent);
                    break;
                default:
                    break;
            }
        }
    }

    private Intent getIndoorMapIntent(int floor, double lon, double lat) {
        Intent getIndoorMapIntent =
                new Intent(MainActivity.this, GetIndoorMapService.class);
        getIndoorMapIntent.putExtra("floor", String.valueOf(floor));
        getIndoorMapIntent.putExtra("longitude", String.valueOf(lon));
        getIndoorMapIntent.putExtra("latitude", String.valueOf(lat));
        return getIndoorMapIntent;
    }


    /**
     * Update the position of location icon
     */
    private void updateLocOverlay() {
        mMapView.getOverlays().remove(mDLOverlay);
        mMapView.getOverlays().add(mDLOverlay);
    }

    /**
     * Open indoor positioning module
     */
    private void startIndoorLoc() {
        isCanReadNfc = true;    // Open NFC module
        // Abre o módulo de posicionamento inercial
        ClientPos ClientPos = new ClientPos(NowClientPos.getNowLatitude(), NowClientPos.getNowLongitude());
        inertialLocIntent.putExtra("init_pos", ClientPos);
        startService(inertialLocIntent);
    }

    /**
     * Close indoor positioning module
     */
    private void stopIndoorLoc() {
        isCanReadNfc = false;    // Close NFC module
        stopService(inertialLocIntent);  // Close indoor positioning module
    }

    private boolean isNowNav = false;

    /**
     * Adiciona o evento de clique correspondente para o controle
     *
     * @param v visualizar
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.locate_fab:   // Mover para a localização atual do usuário
                double longitude = NowClientPos.getNowLongitude();
                double latitude = NowClientPos.getNowLatitude();
                GeoPoint geoPoint = new GeoPoint(latitude, longitude);
                mController.animateTo(geoPoint);
                stopIndoorLoc();
                break;
            case R.id.seek_button:
                String destAreaName = seekEdit.getText().toString();
                if (destAreaName.length() == 0) {
                    Toast.makeText(this, "Destino de navegação não inserido", Toast.LENGTH_SHORT).show();
                    break;
                }
                Intent navigationIntent
                        = new Intent(MainActivity.this, NavigationService.class);
                if (isNowNav) {
                    stopService(navigationIntent);
                    isNowNav = false;
                }

                navigationIntent.putExtra("area_name", destAreaName);
                startService(navigationIntent);
                isNowNav = true;
                break;
            case R.id.building_high:
                AlertDialog.Builder bhDialog = new AlertDialog.Builder(this);
                bhDialog.setTitle("Solicite um mapa interno para o andar atual：");
                bhDialog.setIcon(android.R.drawable.ic_dialog_info);
                final EditText editText = new EditText(getApplicationContext());
                editText.setInputType(InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL);
                editText.setHint("Por favor, insira o andar (por exemplo: 2)");
                editText.setGravity(Gravity.CENTER);
                bhDialog.setView(editText);
                bhDialog.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDrawIndoorMap.removeIndoorMap();
                        Intent getIndoorMapIntent =
                                getIndoorMapIntent(2, NowClientPos.getNowLongitude(), NowClientPos.getNowLatitude());
                        startService(getIndoorMapIntent);
                    }
                });
                bhDialog.setNegativeButton("Cancelar", null);
                bhDialog.show();
                break;
            case R.id.wifi_locate:
                // wifi定位的逻辑操作
                Toast.makeText(this, "Usando wi-fi para localizar...", Toast.LENGTH_LONG).show();
                startService(wifiLocateIntent); // Liga o módulo de posicionamento Wifi
                break;
            case R.id.route_line:
                AlertDialog.Builder routeDialog = new AlertDialog.Builder(this);
                @SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(this)
                        .inflate(R.layout.dialog_customize, null);
                routeDialog.setTitle("caminho pedido：");
                routeDialog.setView(dialogView);
                routeDialog.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Operação lógica de planejamento de caminho
                        Toast.makeText(MainActivity.this, "planejamento de caminho em andamento...", Toast.LENGTH_SHORT).show();
                    }
                });
                routeDialog.setNegativeButton("Cancelar", null);
                routeDialog.show();
                break;
        }
    }


    public static final int MAP_INVALIDATE = 1;

    @SuppressLint("HandlerLeak")
    class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MAP_INVALIDATE:
                    mMapView.invalidate();
                    break;
                default:
            }
        }
    }


    @Override
    public void onGlobalLayout() {
        Rect r = new Rect();
        seekEdit.getWindowVisibleDisplayFrame(r);
        int screenHeight = seekEdit.getRootView().getHeight();
        int heightDifference = screenHeight - (r.bottom);
        if (heightDifference > 200) {
            // Quando o teclado virtual é exibido, a caixa de pesquisa e os botões de navegação de pesquisa são opacos, os botões de navegação de pesquisa estão disponíveis e o cursor da caixa de pesquisa aparece
            seekEdit.setCursorVisible(true);
            seekEdit.getBackground().mutate().setAlpha(255);
            btnSeek.setEnabled(true);
            btnSeek.getBackground().mutate().setAlpha(255);
        } else {
            // Quando o teclado virtual está oculto, a caixa de pesquisa e os botões de navegação de pesquisa ficam translúcidos, os botões de navegação de pesquisa ficam indisponíveis e o cursor da caixa de pesquisa desaparece
            seekEdit.setCursorVisible(false);
            seekEdit.getBackground().mutate().setAlpha(153);
            btnSeek.setEnabled(false);
            btnSeek.getBackground().mutate().setAlpha(153);
        }
    }

    private void getPermissions() {
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        // Aplicação única para permissões não aplicadas
        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(posDataTransIntent);
        mLocationManager.removeUpdates(mLocationListener);
        unregisterReceiver(dataReceiver);
        unregisterReceiver(statusChangeReceiver);
    }
}
