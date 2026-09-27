package app.tuxguitar.app.action.impl.edit;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.app.action.impl.caret.TGMoveToAction;
import app.tuxguitar.app.view.component.tab.Tablature;
import app.tuxguitar.app.view.component.tab.TablatureEditor;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.util.TGContext;

/**
 * While the selection is dragged (Ctrl + drag) the caret follows the mouse to show
 * where the beats will be dropped; the selection itself is left alone.
 */
public class TGUpdateDragMoveAction extends TGActionBase {

	public static final String NAME = "action.edit.move-selection.update-drag";

	public TGUpdateDragMoveAction(TGContext context) {
		super(context, NAME);
	}

	protected void processAction(TGActionContext context) {
		Tablature tablature = TablatureEditor.getInstance(getContext()).getTablature();
		if (tablature.getSelector().isActive() && tablature.getEditorKit().fillSelection(context, true)) {
			context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_KEEP_SELECTION, true);
			TGActionManager.getInstance(getContext()).execute(TGMoveToAction.NAME, context);
		}
	}
}
