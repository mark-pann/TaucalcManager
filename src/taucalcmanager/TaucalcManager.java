/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package taucalcmanager;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Mark
 */
public class TaucalcManager {

public static final String WORKQUEUENAME = "TauWorkQueue";
public static final String RESULTQUEUENAME = "TauResultQueue";
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.util.concurrent.TimeoutException
     */
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(WORKQUEUENAME, false, false, false, null);
        channel.queueDeclare(RESULTQUEUENAME, false, false, false, null);
        channel.basicQos(1);
        
        
        int calcs = Integer.parseInt(args[0]);
        int triesPerCalc = Integer.parseInt(args[1]);
        int[] results = new int[calcs];
        byte[] task = {
            (byte)(triesPerCalc>>24),
            (byte)(triesPerCalc>>16),
            (byte)(triesPerCalc>>8),
            (byte)(triesPerCalc)
        };
        
        System.out.println("Start Publishing");
        for(int i = 0; i < calcs; i++) {
            channel.basicPublish("", WORKQUEUENAME, null, task);
        }
        System.out.println("Done Publishing");
        GetResponse response;
        System.out.println("Start Gathering results");
        for(int i = 0; i < calcs; i++) {
            System.out.println("Waiting for next result: ( " + i + " )");
            do {
                response = channel.basicGet(RESULTQUEUENAME, true);
            }while(response == null);
            
            results[i] = ByteBuffer.wrap(response.getBody()).getInt();
            System.out.println("Received result: " + results[i]);
        }
        System.out.println("Done gathering results");
        System.out.println("Calculating tau");
        BigDecimal result = sumToTau(sum(results), calcs, triesPerCalc);
        System.out.println("Tau = " + result);
        BigDecimal two = new BigDecimal(2);
        System.out.println("pi = tau/2 = " + result.divide(two , RoundingMode.HALF_UP));
        channel.close();
        connection.close();
    }
    
    
    public static long sum(int[] intArray) {
        long result = 0;
        for(int x: intArray) {result += x;}
        System.out.println(result);
        return result;
    }
    
    public static BigDecimal sumToTau(long summ, int amount, int samples) {
        BigDecimal result = new BigDecimal(summ);
        BigDecimal divisor = new BigDecimal(amount).multiply(new BigDecimal(samples));
        System.out.println(divisor);
        result = result.divide(divisor, 20, RoundingMode.HALF_UP).multiply(new BigDecimal(8));
        return result;
        
    }
}
