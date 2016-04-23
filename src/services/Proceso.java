package services;

import java.net.URI;
import java.util.Map;

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
	
	
	public void ok() {
		
		this.eleccion = Eleccion.ELECCION_PASIVA;
		
	}

	
	public void eleccion() {
		
		if (this.estado == Estado.PARADO) {
			return;
		}
		
		this.eleccion = Eleccion.ELECCION_ACTIVA;
		System.out.println(this.id + " eleccion activa");
				
		while (this.eleccion == Eleccion.ELECCION_ACTIVA) {
						
			for (Map.Entry<Integer, String> entry : this.informacion.entrySet()) {

				System.out.println(this.id + " entry: " + entry.getKey());
				
				if (entry.getKey() <= id) {
					System.out.println(this.id + " entry: " + entry.getKey() + " continue");
					continue;
				}
								
				new Thread(new Runnable() {
					public void run() {
						
						System.out.println(id + " run: " + entry.getKey());
						
						Client client = ClientBuilder.newClient();						
						URI uri = UriBuilder.fromUri("http://" + entry.getValue() + "/Servicio").build();
						WebTarget target = client.target(uri);
	
						target.path("servicio")
								.path("eleccion")
								.queryParam("id", entry.getKey())
								.queryParam("candidato", id)
								.request(MediaType.TEXT_PLAIN)
								.post(null);
						
						return;
						
					}
				}).start();
				
				System.out.println(id + " entry: " + entry.getKey() + " start");
				
			}
			
			synchronized (this) {
				try {
					this.wait(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if (this.eleccion == Eleccion.ELECCION_PASIVA) {
				synchronized (this) {
					try {
						this.wait(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					if (this.eleccion == Eleccion.ACUERDO) {
						System.out.println(id + " acuerdo");
						return;
					} else {
						this.eleccion = Eleccion.ELECCION_ACTIVA;
						System.out.println(id + " eleccion activa");
					}
				}
			} else {
				
				for (Map.Entry<Integer, String> entry : this.informacion.entrySet()) {
					
					System.out.println(this.id + " entry: " + entry.getKey());
					
					new Thread(new Runnable() {
						public void run() {
							
							System.out.println(id + " run: " + entry.getKey());
							
							Client client = ClientBuilder.newClient();
							URI uri = UriBuilder.fromUri("http://" + entry.getValue() + "/Servicio").build();
							WebTarget target = client.target(uri);
		
							target.path("servicio")
								.path("coordinador")
								.queryParam("id", entry.getKey())
								.queryParam("coordinador", id)
								.request(MediaType.TEXT_PLAIN)
								.post(null);
							
							return;
							
						}
					}).start();
					
					System.out.println(id + " entry: " + entry.getKey() + " start");
					
				}
				
				return;
				
			}
		}

		return;
		
	}
	
	
	public void eleccion(int candidato) {
		
		Client client = ClientBuilder.newClient();
		URI uri = UriBuilder.fromUri("http://" + informacion.get(candidato) + "/Servicio").build();
		WebTarget target = client.target(uri);

		target.path("servicio")
			.path("ok")
			.queryParam("id", candidato)
			.request(MediaType.TEXT_PLAIN)
			.post(null);
		
		this.eleccion();
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
			}
			
			try {
				Thread.sleep((long) (Math.random() * 500 + 500));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			String string = informacion.get(coordinador);
			
			if (string == null) {
				System.out.println(this.id + " run error 1");
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
				System.out.println(this.id + " run error 2");
				eleccion();
				continue;
			}
			
			if (resultado.contains("-1")) {
				System.out.println(this.id + " run error 3");
				eleccion();
			}
			
		}
		
	}

}
