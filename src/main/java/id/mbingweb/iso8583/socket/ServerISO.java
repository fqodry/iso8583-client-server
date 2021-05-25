package id.mbingweb.iso8583.socket;

import id.mbingweb.iso8583.helper.ISOUtil;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author fqodry
 */
public class ServerISO {
    
    private static final Integer PORT = 12345;
    private static final Map<String, Integer> mappingDENetworkMsg = new HashMap<String, Integer>();
 
    /* Method ini berfungsi untuk menginisialisasi data element dan panjang tiap
     * -tiap data element yang aktif */
    private static void initMappingDENetworkRequest() {
        /* [data-element] [panjang data element] */
        mappingDENetworkMsg.put("3", 6);
        mappingDENetworkMsg.put("7", 8);
        mappingDENetworkMsg.put("11", 6);
        mappingDENetworkMsg.put("12", 6);
        mappingDENetworkMsg.put("13", 4);
        mappingDENetworkMsg.put("39", 3);
        mappingDENetworkMsg.put("48", 999);
        mappingDENetworkMsg.put("70", 3);
    }
    
    public static void main(String[] args)throws IOException {
        initMappingDENetworkRequest();
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server siap menerima koneksi pada port ["+PORT+"]");
        Socket socket = serverSocket.accept();
        InputStreamReader inStreamReader = new InputStreamReader(socket.getInputStream());
        PrintWriter sendMsg = new PrintWriter(socket.getOutputStream());
 
        int data;
        StringBuilder sb = new StringBuilder();
        int counter = 0;
 
        // tambahan 4 karakter karena msg header adalah 4 digit msg length
        int lengthOfMsg = 4;
        while((data = inStreamReader.read()) != 0) {
            counter++;
            sb.append((char) data);
            if (counter == 4) lengthOfMsg += Integer.valueOf(sb.toString());
 
            // klo panjang msg dari MTI sampai END OF MSG sama dengan nilai
            // header maka lanjutkan ke method processingMsg();
            if (lengthOfMsg == sb.toString().length()) {
                System.out.println("Rec. Msg ["+sb.toString()+"] len ["+sb.toString().length()+"]");
                processingMsg(sb.toString(), sendMsg);
            }
        }
    }
    
    /** Memproses msg yang dikirim oleh client berdasarkan nilai MTI.
     * @param data request msg yang berisi [header 4byte][MTI][BITMAP][DATA ELEMENT]
     * @param sendMsg object printWriter untuk menuliskan msg ke network stream
     */
    private static void processingMsg(String data, PrintWriter sendMsg) {
        // msg.asli tanpa 4 digit msg.header
        String origMsgWithoutMsgHeader = data.substring(4, data.length());
 
        // cek nilai MTI
        if (ISOUtil.findMTI(origMsgWithoutMsgHeader).equalsIgnoreCase("1800")) {
            handleNetworkMsg(origMsgWithoutMsgHeader, sendMsg);
        }
    }
    
    /** Method ini akan memproses network management request dan akan menambahkan
     * 1 data element yaitu data element 39 (response code) 000 ke client/sender
     * @param networkMsg request msg yang berisi [header 4byte][MTI][BITMAP][DATA ELEMENT]
     * @param sendMsg object printWriter untuk menuliskan msg ke network stream
     */
    private static void handleNetworkMsg(String networkMsg, PrintWriter sendMsg) {
        int panjangBitmap = ISOUtil.findLengthOfBitmap(networkMsg);
        String hexaBitmap = networkMsg.substring(4, 4+panjangBitmap);
 
        // hitung bitmap
        String binaryBitmap = ISOUtil.findBinaryBitmapFromHexa(hexaBitmap);
        String[] activeDE = ISOUtil.findActiveDE(binaryBitmap).split(";");
 
        StringBuilder networkResp = new StringBuilder();
 
        // setting MTI untuk reply network request
        networkResp.append("1810");
 
        // untuk reply, DE yang aktif adalah DE[3,7,11,12,13,39,48 dan 70]
        String bitmapReply = ISOUtil.getHexaBitmapFromActiveDE(new int[] {3,7,11,12,13,39,48, 70});
        networkResp.append(bitmapReply);
 
        // index msg dimulai dr (4 digit MTI+panjang bitmap = index DE ke 3)
        int startIndexMsg = 4+ISOUtil.findLengthOfBitmap(networkMsg);
        int nextIndex = startIndexMsg;
        String sisaDefaultDE = "";
 
        // ambil nilai DE yang sama dulu
        for (int i=0;i<activeDE.length;i++) {
            // ambil bit ke 3
            if (activeDE[i].equalsIgnoreCase("3")) {
                nextIndex += mappingDENetworkMsg.get(activeDE[i]);
                networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
                debugMessage(3, networkMsg.substring(startIndexMsg, nextIndex));
            } else if(activeDE[i].equalsIgnoreCase("7")) {
                startIndexMsg = nextIndex;
                nextIndex += mappingDENetworkMsg.get(activeDE[i]);
                networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
                debugMessage(7, networkMsg.substring(startIndexMsg, nextIndex));
            } else if(activeDE[i].equalsIgnoreCase("11")) {
                startIndexMsg = nextIndex;
                nextIndex += mappingDENetworkMsg.get(activeDE[i]);
                networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
                debugMessage(11, networkMsg.substring(startIndexMsg, nextIndex));
            } else if(activeDE[i].equalsIgnoreCase("12")) {
                startIndexMsg = nextIndex;
                nextIndex += mappingDENetworkMsg.get(activeDE[i]);
                networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
                debugMessage(12, networkMsg.substring(startIndexMsg, nextIndex));
            } else if(activeDE[i].equalsIgnoreCase("13")) {
                startIndexMsg = nextIndex;
                nextIndex += mappingDENetworkMsg.get(activeDE[i]);
                networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
                debugMessage(13, networkMsg.substring(startIndexMsg, nextIndex));
            } else if(activeDE[i].equalsIgnoreCase("48")) {
                startIndexMsg = nextIndex;
                // ambil dulu var.len utk DE 48
                int varLen = Integer.valueOf(networkMsg.substring(startIndexMsg, (startIndexMsg+3)));
                // 3 digit utk variabel len
                varLen += 3;
                nextIndex += varLen;
                sisaDefaultDE += networkMsg.substring(startIndexMsg, nextIndex);
                debugMessage(48, networkMsg.substring(startIndexMsg, nextIndex));
            } else if(activeDE[i].equalsIgnoreCase("70")) {
                startIndexMsg = nextIndex;
                nextIndex += mappingDENetworkMsg.get(activeDE[i]);
                sisaDefaultDE += networkMsg.substring(startIndexMsg, nextIndex);
                debugMessage(70, networkMsg.substring(startIndexMsg, nextIndex));
            }
        }
 
        // kasih response kode 39 success
        networkResp.append("000");
        // tambahkan sisa default DE
        networkResp.append(sisaDefaultDE);
 
        // tambahkan length 4 digit utk msg.header
        String msgHeader = "";
        if (networkResp.length() < 10) msgHeader = "000" + networkResp.length();
        if (networkResp.length() < 100 && networkResp.length() >= 10) msgHeader = "00" + networkResp.length();
        if (networkResp.length() < 1000 && networkResp.length() >= 100) msgHeader = "0" + networkResp.length();
        if (networkResp.length() >= 1000) msgHeader = String.valueOf(networkResp.length());
 
        String finalMsg = msgHeader + networkResp.toString();
 
        // send to client
        sendMsg.print(finalMsg);
        sendMsg.flush();
    }
 
    private static void debugMessage(Integer fieldNo, String msg) {
        System.out.println("["+fieldNo+"] ["+msg+"]");
    }
}
