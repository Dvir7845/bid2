import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;



public class ClientHandler extends Thread {

    private Socket socket;
  

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try (
            ObjectOutputStream out =
                new ObjectOutputStream(socket.getOutputStream());

            ObjectInputStream in =
                new ObjectInputStream(socket.getInputStream())
        ) {

            while (true) {

                Request request =
                    (Request) in.readObject();

                Response response =
                    processRequest(request);

                out.writeObject(response);
                out.flush();
            }

        } catch (Exception e) {
            System.out.println("Client disconnected");
        }
    }

    private Response processRequest(Request request) {

        switch (request.getAction()) {

            case PLACE_BID:
            	if(placeBid())
                return new Response(
                    true,
                    "Bid received",
                    null
                );
            	else 
            		return new Response(
                        false,
                        "Ilegle Bid ",
                        null
                    );
            		

            case BUY_NOW:
                return new Response(
                    true,
                    "Purchase completed",
                    null
                );

            case AUTO_BID:
                return new Response(
                    true,
                    "Auto bid activated",
                    null
                );

            default:
                return new Response(
                    false,
                    "Unknown request",
                    null
                );
        }
    }

	private boolean placeBid() {
		
		return false;
	}
}