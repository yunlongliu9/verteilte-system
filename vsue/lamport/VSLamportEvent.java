package vsue.lamport;


public class VSLamportEvent {

	private final VSLamportEventType type;
	private final Object content;


	public VSLamportEvent(VSLamportEventType type, Object content) {
		this.type = type;
		this.content = content;
	}


	public VSLamportEventType getType() {
		return type;
	}

	public Object getContent() {
		return content;
	}

}
