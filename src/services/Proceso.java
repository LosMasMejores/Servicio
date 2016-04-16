package services;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

public class Proceso implements Runnable {

	static public enum Estado {
		CORRIENDO, PARADO
	}

	public class Coordinador {
		String ip;
		int id;
	}

	int id;
	Estado estado;
	Coordinador coordinador;
	
	public Proceso(int id) {
		this.estado = Estado.PARADO;
		this.coordinador = new Coordinador();
		this.id = id;
	}
	
	public int getId() {
		return id;
	}

	public Estado getEstado() {
		return estado;
	}

	public void arrancar() {

		if (this.estado == Estado.PARADO) {
			
			this.estado = Estado.CORRIENDO;
			System.out.println(id + " arrancado");
			
		}

	}

	public void parar() {

		if (this.estado == Estado.CORRIENDO) {
			
			this.estado = Estado.PARADO;
			System.out.println(id + " parado");
			
		}

	}

	public int computar() {

		if (this.estado == Estado.PARADO) {
			return -1;
		} else {
			try {
				
				Thread.sleep((long) (Math.random() * 100 + 200));

			} catch (InterruptedException e) {

				e.printStackTrace();

			}
			return 1;
		}

	}
	
	public void ok() {
		
	}

	public void eleccion() {

	}
	
	public void coordinador() {
		
	}

	public void run() {

		while (true) {

			if (this.estado == Estado.PARADO) {

				return;

			} else {

				try {
					Thread.sleep((long) (Math.random() * 500 + 500));
					System.out.println(id + " corriendo");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

//				Client client = ClientBuilder.newClient();
//				URI uri = UriBuilder.fromUri("http://" + coordinador.ip + "/Servicio").build();
//				WebTarget target = client.target(uri);
//
//				Response response = target.path("servicio").path("computar").queryParam("id", coordinador.id)
//						.request(MediaType.TEXT_PLAIN).get();
//
//				if ((Integer) response.getEntity() == -1 || response.getStatus() != 400) {
//
//					this.eleccion();
//
//				}
			}

		}
	}

}
