package webcamServer;

public class ObjectID {
	private volatile int id;
	private volatile Object object;
	
	public ObjectID() {
		
	}
	
	public ObjectID(int id, Object object) {
		this.id = id;
		this.object = object;
	}
	
	public Object getObject() {
		return object;
	}
	
	public int getID() {
		return id;
	}
}
