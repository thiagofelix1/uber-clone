package com.example.thiagofelix.uber.activty;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.example.thiagofelix.uber.config.ConfiguracaoFirebase;
import com.example.thiagofelix.uber.helper.Local;
import com.example.thiagofelix.uber.helper.UsuarioFirebase;
import com.example.thiagofelix.uber.model.Destino;
import com.example.thiagofelix.uber.model.Requisicao;
import com.example.thiagofelix.uber.model.Usuario;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.thiagofelix.uber.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

public class CorridaActivty extends AppCompatActivity
        implements OnMapReadyCallback {
    private FloatingActionButton fabRota;

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localMotorista;
    private LatLng localPassageiro;
    private Destino destino;
    private Usuario motorista;
    private Usuario passageiro;
    private String idRequisicao;
    private Requisicao requisicao;
    private DatabaseReference firebaseRef;
    private Button botaoAceitarCorrida;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;
    private String statusRequisicao;
    private boolean requisicaoAtiva;
    private String resultado;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corrida_activty);
        inicializarComponentes();

        //recuperar dados do usuario
        if(getIntent().getExtras().containsKey("idRequisicao")
        && getIntent().getExtras().containsKey("motorista")){
            Bundle extras = getIntent().getExtras();
            motorista = (Usuario) extras.getSerializable("motorista");
            localMotorista = new LatLng(
                    Double.parseDouble(motorista.getLongitude()),
                    Double.parseDouble(motorista.getLongitude())
            );
            idRequisicao = extras.getString("idRequisicao");
            requisicaoAtiva = extras.getBoolean("requisicaoAtiva");
            verificaStatusRequisicao();

        }


    }
    public void
    verificaStatusRequisicao(){
        final DatabaseReference requisicoes = firebaseRef.child("requisicoes")
                .child(idRequisicao);
        requisicoes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //recupera requisicao
                requisicao = dataSnapshot.getValue(Requisicao.class);
                if(requisicao != null){
                    passageiro = requisicao.getPassageiro();
                    localPassageiro = new LatLng(
                            Double.parseDouble(passageiro.getLatitude()),
                            Double.parseDouble(passageiro.getLongitude())
                    );
                    statusRequisicao = requisicao.getStatus();
                    destino = requisicao.getDestino();
                    alteraInterfaceStatusRequisicao(statusRequisicao);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    private void alteraInterfaceStatusRequisicao(String status){
        switch (status){
            case Requisicao.STATUS_AGUARDANDO:
                requisicaoAguardando();

                break;
            case Requisicao.STATUS_A_CAMINHO:
                requisicaoACaminho();
                break;
            case Requisicao.STATUS_VIAGEM:
                requisicaoViagem();
                break;
            case Requisicao.STATUS_FINALIZADA:
                requisicaoFinalizada();
                break;
            case Requisicao.STATUS_CANCELADA:
                requisicaoCancelada();
                break;
        }
    }
    private void requisicaoCancelada(){
        Toast.makeText(this,"corrida cancelada pelo passageiro",Toast.LENGTH_LONG).show();
        startActivity(new Intent(CorridaActivty.this,RequisicoesActivty.class));
    }

    private void requisicaoAguardando(){
        botaoAceitarCorrida.setText("Aceitar corrida");

        //exibe marcador do motorista
        adicionarMarcadorMotorista(localMotorista,motorista.getNome());

        centralizarMarcador(localMotorista);
    }
    @SuppressLint("RestrictedApi")
    private void requisicaoACaminho(){
        botaoAceitarCorrida.setText("A caminho do passageiro");
        fabRota.setVisibility(View.VISIBLE);
        //exibe marcador do motorista
        adicionarMarcadorMotorista(localMotorista,motorista.getNome());
        //exibe marcador do passageiro
        adicionarMarcadorPassageiro(localPassageiro,passageiro.getNome());
        //centralizar dois marcadores
        centralizarDoisMarcadores(marcadorMotorista,marcadorPassageiro);
        //inicia monitoramento do motorista e do passageiro
        iniciarMonitoramento(motorista, localPassageiro,Requisicao.STATUS_VIAGEM);
    }

    @SuppressLint("RestrictedApi")
    private void requisicaoViagem(){
        //alterar interface
        fabRota.setVisibility(View.VISIBLE);
        botaoAceitarCorrida.setText("A caminho do destino");
        //exibe marcador do motorista
        adicionarMarcadorMotorista(localMotorista,motorista.getNome());
        //exibe marcador de destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionarMarcadorDestino(localDestino,"Destino");
        //centralizar destino e motorista
        centralizarDoisMarcadores(marcadorMotorista,marcadorDestino);
        //iniciar monitoramento motorista destinoalterarStatus
        iniciarMonitoramento(motorista,localDestino,Requisicao.STATUS_FINALIZADA);

    }
    @SuppressLint("RestrictedApi")
    public void requisicaoFinalizada(){
        fabRota.setVisibility(View.GONE);
        requisicaoAtiva = false;
        if(marcadorMotorista != null)
            marcadorMotorista.remove();

        if(marcadorDestino != null)
            marcadorDestino.remove();
        //exibe marcador de destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionarMarcadorDestino(localDestino,"Destino");
        centralizarMarcador(localDestino);


        //calcular distancia
        float distancia = Local.calcularDistancia(localPassageiro,localDestino);
        float valorMinimo = 5;
        if(distancia < 2){
             resultado = String.valueOf(valorMinimo);
        }else {
            float valorkm = valorMinimo + (distancia-2)*3;
            DecimalFormat decimal = new DecimalFormat("0.00");
            resultado = decimal.format(valorkm);
        }

        botaoAceitarCorrida.setText("Corrida finalizada - R$ "+resultado);

    }
    private void centralizarMarcador(LatLng local){
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(local,20)
        );
    }

    private  void iniciarMonitoramento(final Usuario uOrigem, LatLng localDestino, final String status ){

        //inicializar Geofire
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child("local_usuario");
        GeoFire geoFire = new GeoFire(localUsuario);

        //adicionar circulo no passageiro
        final Circle circulo = mMap.addCircle(
                new CircleOptions()
                .center(localDestino)
                .radius(200)//em metros
                .fillColor(Color.argb(90,225,153,0))
                .strokeColor(Color.argb(190,255,153,0))
        );
        final GeoQuery geoQuery = geoFire.queryAtLocation(
                new GeoLocation(localDestino.latitude, localDestino.longitude),
                0.2//em km

        );
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                 if(key.equals(uOrigem.getId())){
                   // Log.d("onKeyEntered","onKeyEntered motorista está dentro da área");
                    //altera status da requisição para em viagem
                    requisicao.setStatus(status);
                    requisicao.atualizarStatus();
                    //remove o listener
                    geoQuery.removeAllListeners();
                    circulo.remove();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }
    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(marcador1.getPosition());
        builder.include(marcador2.getPosition());

        LatLngBounds bounds = builder.build();
        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura  = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura*0.2);
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds,largura,altura,espacoInterno)
        );
    }

    private void adicionarMarcadorMotorista(LatLng localizacao, String titulo){
            if(marcadorMotorista != null)
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro))

        );

    }
    private void adicionarMarcadorPassageiro(LatLng localizacao, String titulo){
        if(marcadorPassageiro != null)
            marcadorPassageiro.remove();

            marcadorPassageiro = mMap.addMarker(
                    new MarkerOptions()
                            .position(localizacao)
                            .title(titulo)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))

            );
    }
    private void adicionarMarcadorDestino(LatLng localizacao, String titulo){

        if(marcadorPassageiro != null)
            marcadorPassageiro.remove();

        if(marcadorDestino != null)
            marcadorDestino.remove();

        marcadorDestino = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino))

        );

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //recuperar localização
        recuperarLocalizacaoUsuario();


    }
    private void recuperarLocalizacaoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //recuperar latitude / longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localMotorista = new LatLng(latitude, longitude);

                //atualizar dados do Geofire
                UsuarioFirebase.atualizarDadosLocaliacao(latitude,longitude);
                //atualizar localização motorista no firebase
                motorista.setLatitude(String.valueOf(latitude));
                motorista.setLongitude(String.valueOf(longitude));
                requisicao.setMotorista(motorista);
                requisicao.atualizarLocalizacaoMotorista();

                alteraInterfaceStatusRequisicao(statusRequisicao);


            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };


        //solicitar atualizações do localização
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    locationManager.GPS_PROVIDER,
                    10000,
                    10,
                    locationListener

            );
            return;
        }

    }

    public void aceitarCorrida(View view){
        requisicao = new Requisicao();
        requisicao.setId(idRequisicao);
        requisicao.setMotorista(motorista);
        requisicao.setStatus(Requisicao.STATUS_A_CAMINHO);
        requisicao.atualizar();
    }

    private void inicializarComponentes(){
        fabRota = findViewById(R.id.fabRota);
        fabRota.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String status = statusRequisicao;
                if(status !=null && !status.isEmpty()){
                    String lat = "";
                    String lon = "";

                    switch (status){
                        case Requisicao.STATUS_A_CAMINHO:
                            lat = String.valueOf(localPassageiro.latitude);
                            lon = String.valueOf(localPassageiro.longitude);
                            break;
                        case Requisicao.STATUS_VIAGEM:
                            lat = destino.getLatitude();
                            lon = destino.getLongitude();
                            break;
                    }

                    //abrir rota
                    String latlong = lat + "," + lon;
                    Uri uri = Uri.parse("google.navigation:q="+latlong+"&mode=d");
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);

                }
            }
        });
        botaoAceitarCorrida = findViewById(R.id.buttomAceitarCorrida);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("iniciar corrida");

        // confirar mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if(requisicaoAtiva){
            Toast.makeText(CorridaActivty.this,"Necessário encerrar a requisição atual",Toast.LENGTH_LONG).show();

        }else{
            Intent i = new Intent(CorridaActivty.this,RequisicoesActivty.class);
            startActivity(i);
        }
        //verifica o status da requisição para encerrar
        if(statusRequisicao != null && !statusRequisicao.isEmpty()){
            requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
            requisicao.atualizarStatus();
        }
        return false;
    }
}
