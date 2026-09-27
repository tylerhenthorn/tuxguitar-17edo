package app.tuxguitar.ui.event;
import app.tuxguitar.ui.UIComponent;
import app.tuxguitar.ui.resource.UIPosition;

public class UIMouseEvent extends UIEvent {

	private Integer button;
	private UIPosition position;
	private Boolean isShiftDown;
	private Boolean isControlDown;

	public UIMouseEvent(UIComponent control, UIPosition position, Integer button, Boolean isShiftDown) {
		this(control, position, button, isShiftDown, Boolean.FALSE);
	}

	public UIMouseEvent(UIComponent control, UIPosition position, Integer button, Boolean isShiftDown, Boolean isControlDown) {
		super(control);

		this.button = button;
		this.position = position;
		this.isShiftDown = isShiftDown;
		this.isControlDown = isControlDown;
	}

	public UIPosition getPosition() {
		return position;
	}

	public Integer getButton() {
		return button;
	}

	public Boolean isShiftDown()  {
		return isShiftDown;
	}

	public Boolean isControlDown()  {
		return isControlDown;
	}
}
