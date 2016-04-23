package services;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientProperties;

public class Proceso implements Runnable {

	static public enum Estado {
		CORRIENDO, PARADO
	}
	
	static public enum Eleccion {
		ACUERDO, ELECCION_ACTIVA, ELECCION_PASIVA
	}

	int id, coordinador;
	Estado estado;
	Eleccion eleccion;
	Map<Integer, String> informacion;
	
	
	public Proceso(int id, Map<Integer, String> informacion) {
		
		this.id = id;
		this.coordinador = 0;
		this.estado = Estado.PARADO;
		this.eleccion = Eleccion.ACUERDO;
		this.informacion = informacion;
		
	}
	
	
	public int getId() {
		
		return this.id;
		
	}
	

	public Estado getEstado() {
		
		return this.estado;
		
	}

	
	public void arrancar() {

		this.estado = Estado.CORRIENDO;
		System.out.println(this.id + " arrancado");

	}

	
	public void parar() {

		this.estado = Estado.PARADO;
		System.out.println(this.id + " parado");

	}

	
	public String computar() {
		
		if (this.estado == Estado.PARADO) {
			return "-1";
		} else {
			try {
				Thread.sleep((long) (Math.random() * 100 + 200));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
//			System.out.println(this.id + " computar");

			return "1";
		}

	}
	
	
	public boolean ok() {
		
		if (this.estado == Estado.CORRIENDO) {
			System.out.println(this.id + " ok");
			return true;
		} else {
			return false;
		}
		
	}

	
	public void eleccion() {
		
		if (this.estado == Estado.PARADO) {
			return;
		}
		
		this.eleccion = Eleccion.ELECCION_ACTIVA;
		System.out.println(this.id + " eleccion activa");
		
		int size = this.informacion.size();
		System.out.println(this.id + " size: " + size);
		
		if (size <= 1) {
			System.out.println(this.id + " size <= 1");
			this.coordinador(this.id);
			return;
		}
		
		System.out.println(this.id + " new semaphore");
		
		while (this.eleccion == Eleccion.ELECCION_ACTIVA) {
			
			Semaphore sem = new Semaphore(size);
			
			for (Map.Entry<Integer, String> entry : this.informacion.entrySet()) {

				System.out.println(this.id + " entry: " + entry.getKey());
				
				if (entry.getKey() <= id) {
					System.out.println(this.id + " entry: " + entry.getKey() + " continue");
					continue;
				}
				
				try {
					sem.acquire();
					System.out.println(this.id + " entry: " + entry.getKey() + " sem.adquire");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
								
				new Thread(new Runnable() {
					public void run() {
						
						System.out.println(id + " run: " + entry.getKey());
						
						Client client = ClientBuilder.newClient();
						client.property(ClientProperties.CONNECT_TIMEOUT, 1000);
						client.property(ClientProperties.READ_TIMEOUT, 1000);
						
						URI uri = UriBuilder.fromUri("http://" + entry.getValue() + "/Servicio").build();
						WebTarget target = client.target(uri);
	
						Response response = target.path("servicio")
								.path("eleccion")
								.queryParam("id", entry.getKey())
								.queryParam("candidato", id)
								.request(MediaType.TEXT_PLAIN)
								.post(null);
						
						if (response.getStatus() != 200) {
							System.out.println(id + " response: " + entry.getKey() + " status != 200");
							sem.release();
							System.out.println(id + " entry: " + entry.getKey() + " sem.release");
							return;
						}
						
						eleccion = Eleccion.ELECCION_PASIVA;
						System.out.println(id + " eleccion pasiva");
						
						sem.release();
						System.out.println(id + " entry: " + entry.getKey() + " sem.release");
						return;
					}
				}).start();
				System.out.println(id + " entry: " + entry.getKey() + " start");
			}
			
			try {
				sem.acquire(size);
				System.out.println(this.id + " sem.adquire: " + size);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (this.eleccion == Eleccion.ELECCION_PASIVA) {
				synchronized (this) {
					try {
						this.wait(1000);
						if (this.eleccion == Eleccion.ACUERDO) {
							System.out.println(id + " acuerdo");
							return;
						} else {
							this.eleccion = Eleccion.ELECCION_ACTIVA;
							System.out.println(id + " eleccion activa");
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} else {
				this.coordinador(this.id);
				
				for (Map.Entry<Integer, String> entry : this.informacion.entrySet()) {
					
					System.out.println(this.id + " entry: " + entry.getKey());
					
					if (entry.getKey() == this.id) {
						System.out.println(this.id + " entry: " + entry.getKey() + " continue");
						continue;
					}
					
					Client client = ClientBuilder.newClient();
					
					URI uri = UriBuilder.fromUri("http://" + entry.getValue() + "/Servicio").build();
					WebTarget target = client.target(uri);

					Response response = target.path("servicio")
							.path("coordinador")
							.queryParam("id", entry.getKey())
							.queryParam("coordinador", this.coordinador)
							.request(MediaType.TEXT_PLAIN)
							.post(null);
				}
			}
		}

		return;
		
	}
	
	
	public void coordinador(int coordinador) {
		
		this.coordinador = coordinador;
		System.out.println(this.id + " coordinador: " + coordinador);
		this.eleccion = Eleccion.ACUERDO;
		System.out.println(this.id + " acuerdo");
		return;
		
	}

	
	public void run() {

		while (true) {
			if (this.estado == Estado.PARADO) {
				return;
			} else {
				try {
					Thread.sleep((long) (Math.random() * 500 + 500));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				String string = informacion.get(coordinador);
				
				if (string == null) {
					System.out.println(this.id + " run 1");
					eleccion();
					continue;
				}
				
				Client client = ClientBuilder.newClient();
				
				URI uri = UriBuilder.fromUri("http://" + string + "/Servicio").build();
				WebTarget target = client.target(uri);

				Response response = target.path("servicio")
						.path("computar")
						.queryParam("id", coordinador)
						.request(MediaType.TEXT_PLAIN)
						.get();

				String resultado = response.readEntity(String.class);
				
				if (resultado == null || response.getStatus() != 200) {
					System.out.println(this.id + " run 2");
					eleccion();
					continue;
				}
				
				if (resultado.contains("-1")) {
					System.out.println(this.id + " run 3");
					eleccion();
				}
			}
		}
		
	}

}
