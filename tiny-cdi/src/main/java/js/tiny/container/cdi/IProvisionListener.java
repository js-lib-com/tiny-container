package js.tiny.container.cdi;

public interface IProvisionListener<T> {

	void onProvision(IProvisionInvocation<T> invocation);

}
