package net.sumik.sync.client.gui.widget;

import net.sumik.sync.client.gui.TooltipProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.Date;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractWidget extends AbstractContainerEventHandler implements Renderable, NarratableEntry, TooltipProvider, GuiEventListener {
    public boolean visible = true;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private double lastMouseX = -1;
    private double lastMouseY = -1;
    private long lastMovementTime = 0;

    public boolean isHovered() {
        return this.isHovered;
    }

    public boolean isPressed() {
        return this.isPressed;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (this.visible) {
            this.mouseMoved(mouseX, mouseY);
            this.renderContent(guiGraphics, mouseX, mouseY, delta);
        }
    }

    protected abstract void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta);

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.isHovered = this.visible && this.isMouseOver(mouseX, mouseY);
        this.isPressed &= this.isHovered;
        if (!this.isHovered || this.lastMouseX != mouseX || this.lastMouseY != mouseY) {
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            this.lastMovementTime = new Date().getTime();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.visible && this.isHovered) {
            this.isPressed = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.visible && this.isPressed) {
            this.onMouseClick(mouseX, mouseY, button);
            this.isPressed = false;
            return true;
        }
        return false;
    }

    protected void onMouseClick(double mouseX, double mouseY, int button) { }

    @Override
    public NarrationPriority narrationPriority() {
        return this.isPressed() ? NarrationPriority.FOCUSED : this.isHovered() ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    @Override
    public Component getTooltip() {
        if (this.isHovered && (new Date().getTime() - this.lastMovementTime) >= this.getTooltipDelay()) {
            return this.getWidgetDescription();
        }

        return null;
    }

    protected long getTooltipDelay() {
        return 500;
    }

    protected Component getWidgetDescription() {
        return null;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        Component description = this.getWidgetDescription();
        if (description != null) {
            narrationElementOutput.add(NarratedElementType.TITLE, description);
        }
    }
}