package js.tiny.container.contextparam;

class BusinessClass {
	@ContextParam("static.field")
	static String staticField;

	@ContextParam(value = "static.mandatory.field", mandatory = true)
	static String staticMandatoryField;

	@ContextParam("instance.field")
	String instanceField;

	@ContextParam(value = "instance.mandatory.field", mandatory = true)
	String instanceMandatoryField;
}
