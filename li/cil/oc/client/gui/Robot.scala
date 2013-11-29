package li.cil.oc.client.gui

import java.util
import li.cil.oc.Settings
import li.cil.oc.client.renderer.MonospaceFontRenderer
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.util.{StatCollector, ResourceLocation}
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

class Robot(playerInventory: InventoryPlayer, val robot: tileentity.Robot) extends GuiContainer(new container.Robot(playerInventory, robot)) with Buffer {
  xSize = 256
  ySize = 242

  private val robotBackground = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot.png")
  protected val powerIcon = new ResourceLocation(Settings.resourceDomain, "textures/gui/power.png")
  private val selection = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot_selection.png")

  protected var powerButton: ImageButton = _

  protected val buffer = robot.buffer

  private val bufferWidth = 242.0
  private val bufferHeight = 128.0
  private val bufferMargin = BufferRenderer.innerMargin

  private val inventoryX = 176
  private val inventoryY = 140

  private val powerX = 28
  private val powerY = 142

  private val powerWidth = 140
  private val powerHeight = 12

  private val selectionSize = 20
  private val selectionsStates = 17
  private val selectionStepV = 1 / selectionsStates.toDouble

  def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  protected override def actionPerformed(button: GuiButton) {
    if (button.id == 0) {
      ClientPacketSender.sendComputerPower(robot, !robot.isRunning)
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    powerButton.toggled = robot.isRunning
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
    powerButton = new ImageButton(0, guiLeft + 7, guiTop + 139, 18, 18, powerIcon)
    add(buttonList, powerButton)
  }

  override def drawSlotInventory(slot: Slot) {
    RenderState.makeItBlend()
    super.drawSlotInventory(slot)
    GL11.glDisable(GL11.GL_BLEND)
  }

  def drawBuffer() {
    GL11.glTranslatef(guiLeft + 8, guiTop + 8, 0)
    RenderState.disableLighting()
    RenderState.makeItBlend()
    BufferRenderer.drawText()
  }

  protected override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
    if (isPointInRegion(powerX, powerY, powerWidth, powerHeight, mouseX, mouseY)) {
      GL11.glPushAttrib(0xFFFFFFFF) // Me lazy... prevents NEI render glitch.
      val tooltip = new java.util.ArrayList[String]
      val format = StatCollector.translateToLocal(Settings.namespace + "text.Robot.Power") + ": %d%% (%d/%d)"
      tooltip.add(format.format(
        ((robot.globalBuffer / robot.globalBufferSize) * 100).toInt,
        robot.globalBuffer.toInt,
        robot.globalBufferSize.toInt))
      drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
      GL11.glPopAttrib()
    }
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    mc.renderEngine.bindTexture(robotBackground)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    drawPowerLevel()
    drawSelection()
  }

  protected override def keyTyped(char: Char, code: Int) {
    if (code == Keyboard.KEY_ESCAPE) {
      super.keyTyped(char, code)
    }
  }

  protected def changeSize(w: Double, h: Double) = {
    val bw = w * MonospaceFontRenderer.fontWidth
    val bh = h * MonospaceFontRenderer.fontHeight
    val scaleX = (bufferWidth / (bw + bufferMargin * 2.0)) min 1
    val scaleY = (bufferHeight / (bh + bufferMargin * 2.0)) min 1
    scaleX min scaleY
  }

  private def drawSelection() {
    RenderState.makeItBlend()
    Minecraft.getMinecraft.renderEngine.bindTexture(selection)
    val now = System.currentTimeMillis() / 1000.0
    val offsetV = ((now - now.toInt) * selectionsStates).toInt * selectionStepV
    val slot = robot.selectedSlot - robot.actualSlot(0)
    val x = guiLeft + inventoryX + (slot % 4) * (selectionSize - 2)
    val y = guiTop + inventoryY + (slot / 4) * (selectionSize - 2)

    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y, zLevel, 0, offsetV)
    t.addVertexWithUV(x, y + selectionSize, zLevel, 0, offsetV + selectionStepV)
    t.addVertexWithUV(x + selectionSize, y + selectionSize, zLevel, 1, offsetV + selectionStepV)
    t.addVertexWithUV(x + selectionSize, y, zLevel, 1, offsetV)
    t.draw()
  }

  private def drawPowerLevel() {
    val level = robot.globalBuffer / robot.globalBufferSize

    val u0 = 0
    val u1 = powerWidth / 256.0 * level
    val v0 = 1 - powerHeight / 256.0
    val v1 = 1
    val x = guiLeft + powerX
    val y = guiTop + powerY
    val w = powerWidth * level

    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y, zLevel, u0, v0)
    t.addVertexWithUV(x, y + powerHeight, zLevel, u0, v1)
    t.addVertexWithUV(x + w, y + powerHeight, zLevel, u1, v1)
    t.addVertexWithUV(x + w, y, zLevel, u1, v0)
    t.draw()
  }
}