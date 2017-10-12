package br.com.bhl.superfid.controller;

/**
 * Created by hericlespontes on 15/07/17.
 */
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

import br.com.bhl.superfid.view.ParearBluetoothActivity;

public class BluetoothConnectionThread extends Thread {

    private BluetoothSocket btSocket = null;
    private BluetoothServerSocket btServerSocket = null;
    private InputStream input = null;
    private OutputStream output = null;
    private String btDevAddress = null;
    private String btDevPwd = null;
    private String myUUID = "00001101-0000-1000-8000-00805F9B34FB";
    private boolean server;
    private boolean running = false;

    IntentFilter filter2 = new IntentFilter(
            "android.bluetooth.device.action.PAIRING_REQUEST");


    /*  Este construtor prepara o dispositivo para atuar como servidor.
     */
    public BluetoothConnectionThread() {

        this.server = true;
    }

    /*  Este construtor prepara o dispositivo para atuar como cliente.
        Tem como argumento uma string contendo o endereço MAC do dispositivo
    Bluetooth para o qual deve ser solicitada uma conexão.
     */
    public BluetoothConnectionThread(String btDevAddress, String btDevPwd) {

        this.server = false;
        this.btDevAddress = btDevAddress;
        this.btDevPwd = btDevPwd;



    }

    /*  O método run() contem as instruções que serão efetivamente realizadas
    em uma nova thread.
     */
    public void run() {

        /*  Anuncia que a thread está sendo executada.
            Pega uma referência para o adaptador Bluetooth padrão.
         */
        this.running = true;
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        /*  Determina que ações executar dependendo se a thread está configurada
        para atuar como servidor ou cliente.
         */
        if (this.server) {

            /*  Servidor.
             */
            try {

                /*  Cria um socket de servidor Bluetooth.
                    O socket servidor será usado apenas para iniciar a conexão.
                    Permanece em estado de espera até que algum cliente
                estabeleça uma conexão.
                 */
                btServerSocket = btAdapter.listenUsingRfcommWithServiceRecord("Super Bluetooth", UUID.fromString(myUUID));
                btSocket = btServerSocket.accept();

                /*  Se a conexão foi estabelecida corretamente, o socket
                servidor pode ser liberado.
                 */
                if (btSocket != null) {

                    btServerSocket.close();
                }

            } catch (IOException e) {

                /*  Caso ocorra alguma exceção, exibe o stack trace para debug.
                    Envia um código para a Activity principal, informando que
                a conexão falhou.
                 */
                e.printStackTrace();
                toMainActivity("---N".getBytes());
            }


        } else {

            /*  Cliente.
             */
            try {

                /*  Obtem uma representação do dispositivo Bluetooth com
                endereço btDevAddress.
                    Cria um socket Bluetooth.
                 */
                BluetoothDevice btDevice = btAdapter.getRemoteDevice(btDevAddress);
                if (btDevice.getBondState() == BluetoothDevice.BOND_NONE) {

                }

                btSocket = btDevice.createRfcommSocketToServiceRecord(UUID.fromString(myUUID));

                /*  Envia ao sistema um comando para cancelar qualquer processo
                de descoberta em execução.
                 */
                btAdapter.cancelDiscovery();

                /*  Solicita uma conexão ao dispositivo cujo endereço é
                btDevAddress.
                    Permanece em estado de espera até que a conexão seja
                estabelecida.
                 */
                if (btSocket != null)
                    btSocket.connect();

            } catch (IOException e) {

                /*  Caso ocorra alguma exceção, exibe o stack trace para debug.
                    Envia um código para a Activity principal, informando que
                a conexão falhou.
                 */
                e.printStackTrace();
                toMainActivity("---N".getBytes());
            }

        }

        /*  Pronto, estamos conectados! Agora, só precisamos gerenciar a conexão.
            ...
         */

        if (btSocket != null) {



            /*  Envia um código para a Activity principal informando que a
            a conexão ocorreu com sucesso.
             */
            toMainActivity("---S".getBytes());

            try {

                /*  Obtem referências para os fluxos de entrada e saída do
                socket Bluetooth.
                 */
                input = btSocket.getInputStream();
                output = btSocket.getOutputStream();

                /*  Cria um byte array para armazenar temporariamente uma
                mensagem recebida.
                    O inteiro bytes representará o número de bytes lidos na
                última mensagem recebida.
                 */
                byte[] buffer = new byte[1024];
                int bytes;

                /*  Permanece em estado de espera até que uma mensagem seja
                recebida.
                    Armazena a mensagem recebida no buffer.
                    Envia a mensagem recebida para a Activity principal, do
                primeiro ao último byte lido.
                    Esta thread permanecerá em estado de escuta até que
                a variável running assuma o valor false.
                 */

                //envia caracter de confirmacao para o hardware
                byte[] init = "1".getBytes();
                write(init);

                while (running) {

                    bytes = input.read(buffer);

                    toMainActivity(Arrays.copyOfRange(buffer, 0, bytes));

                }

            } catch (IOException e) {

                /*  Caso ocorra alguma exceção, exibe o stack trace para debug.
                    Envia um código para a Activity principal, informando que
                a conexão falhou.
                 */
                e.printStackTrace();
                toMainActivity("---N".getBytes());
            }
        }

    }

    /*  Utiliza um handler para enviar um byte array à Activity principal.
        O byte array é encapsulado em um Bundle e posteriormente em uma Message
    antes de ser enviado.
     */
    private void toMainActivity(byte[] data) {

        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putByteArray("data", data);
        message.setData(bundle);
        ParearBluetoothActivity.handler.sendMessage(message);
    }

    /*  Método utilizado pela Activity principal para transmitir uma mensagem ao
     outro lado da conexão.
        A mensagem deve ser representada por um byte array.
     */
    public void write(byte[] data) {

        if (output != null) {
            try {

                /*  Transmite a mensagem.
                 */
                output.write(data);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            /*  Envia à Activity principal um código de erro durante a conexão.
             */
            toMainActivity("---N".getBytes());
        }
    }

    /*  Método utilizado pela Activity principal para encerrar a conexão
     */
    public void cancel() {

        try {

            running = false;
            btServerSocket.close();
            btSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        running = false;
    }



}