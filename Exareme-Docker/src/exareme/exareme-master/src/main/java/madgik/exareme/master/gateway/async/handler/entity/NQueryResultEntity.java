package madgik.exareme.master.gateway.async.handler.entity;

import madgik.exareme.master.client.AdpDBClientQueryStatus;
import madgik.exareme.master.connector.DataSerialization;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * TODO flush output before suspend
 */
public class NQueryResultEntity extends BasicHttpEntity implements HttpAsyncContentProducer {

    private static final Logger log = Logger.getLogger(NQueryResultEntity.class);
    
    private final AdpDBClientQueryStatus queryStatus;
    private final ByteBuffer buffer;
    private ReadableByteChannel channel;
    private NQueryStatusEntity.QueryStatusListener l;
    private DataSerialization format;

    public NQueryResultEntity(AdpDBClientQueryStatus status, DataSerialization ds,
                              int bufferSize) {
        super();
        queryStatus = status;
        buffer = ByteBuffer.allocate(bufferSize);
        channel = null;
        l = null;
        format = ds;
    }


    @Override
    public void produceContent(ContentEncoder encoder, IOControl ioctrl)
            throws IOException {

        if (!queryStatus.hasFinished() && !queryStatus.hasError()) {
            if (l == null) {
                l = new NQueryStatusEntity.QueryStatusListener(ioctrl);
                queryStatus.registerListener(l);
            }
            ioctrl.suspendOutput();
            return;
        }

        if (!queryStatus.hasError()) {
            if (channel == null) {
                channel = Channels.newChannel(queryStatus.getResult(format));
            }
            channel.read(buffer);
            buffer.flip();
            int i = encoder.write(buffer);
            final boolean buffering = this.buffer.hasRemaining();
            this.buffer.compact();
            if (i < 1 && !buffering) {
                encoder.complete();
                close();
            }
        } else {
            log.trace("|" + queryStatus.getError() + "|");
            if (queryStatus.getError().contains("ExaremeError:")) {
                String data = queryStatus.getError().substring(result.lastIndexOf("ExaremeError:") + "ExaremeError:".length()).replaceAll("\\s"," ");
                String type = "text/plain+error";
                String result = defaultOutputFormat(data,type);
                encoder.write(ByteBuffer.wrap(result.getBytes()));
                encoder.complete();
                close();
            }
            else if (queryStatus.getError().contains("PrivacyError")) {
                String data = "The Experiment could not run with the input provided because there are insufficient data.";
                String type = "text/plain+warning";
                String result = defaultOutputFormat(data,type);
                encoder.write(ByteBuffer.wrap(result.getBytes()));
                encoder.complete();
                close();
            }
            else if (queryStatus.getError().contains("java.rmi.RemoteException")) {
                String data = "One or more containers are not responding. Please inform the system administrator.");
                String type = "text/plain+error";
                String result = defaultOutputFormat(data,type);
                encoder.write(ByteBuffer.wrap(result.getBytes()));
                encoder.complete();
                close();
            }
            else if(queryStatus.getError().contains("java.lang.IndexOutOfBoundsException:")){
                String data = "Something went wrong. Clean-ups were made, you may re-run your experiment. Please inform the system administrator though for fixing any remaining issue.";
                String type = "text/plain+error";
                String result = defaultOutputFormat(data,type);
                encoder.write(ByteBuffer.wrap(result.getBytes()));
                encoder.complete();
                close();
            }
            else {
                String data="Something went wrong. Please inform your system administrator to consult the logs.";
                String type ="text/plain+error";
                String result = defaultOutputFormat(data,type);
                encoder.write(ByteBuffer.wrap(result.getBytes()));
                encoder.complete();
                close();
            }
        }
    }
    private String defaultOutputFormat(String data, String type){
        return "{\"result\" : [{\"data\":"+"\""+data+"\",\"type\":"+"\""+type+"\"}]}";
    }

    @Override
    public void close() throws IOException {
        queryStatus.close();
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }
}
