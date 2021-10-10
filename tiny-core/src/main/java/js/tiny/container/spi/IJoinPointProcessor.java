package js.tiny.container.spi;

/**
 * A join point is a location in a program where the control flow can arrive via multiple paths: in our case container services
 * and application logic. When container flow reach a join point it should executes specific container service logic. For this,
 * every container service implements a join point processor interface - a specialization of this interface.
 * 
 * @author Iulian Rotaru
 */
public interface IJoinPointProcessor extends IContainerService {

	default int getPriority() {
		return -1;
	}

}
