package webcamServer;

public class ObjectID {
	public volatile int id;
	public volatile Object object;
	
	public ObjectID() {
		
	}
	
	public ObjectID(int id, Object object) {
		this.id = id;
		this.object = object;
	}
}
