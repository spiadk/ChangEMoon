package com.moon.change

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class MainActivity : Activity() {
    private lateinit var game: MoonGame
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game = MoonGame(this)
        game.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }
        setContentView(game)
    }
    override fun onPause() { game.suspendGame(); super.onPause() }
    override fun onResume() { super.onResume(); if (::game.isInitialized) game.resumeFrames() }
}

/** Pure native Canvas game. Coordinates use a 400 x 800 letterboxed logical stage. */
class MoonGame(context: android.content.Context) : View(context) {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gold = Color.rgb(244, 210, 144)
    private val white = Color.rgb(247, 240, 230)
    private val muted = Color.rgb(168, 179, 211)
    private val prefs = context.getSharedPreferences("moon_rewards", 0)
    private var cakes = prefs.getInt("cakes", 0)
    private var level = prefs.getInt("level", 1)
    private var screen = "home"
    private var paused = false
    private var active = true
    private var time = 40f
    private var hits = 0
    private var phase = 0f
    private var moonX = 200f
    private var moonY = 245f
    private var angle = -Math.PI.toFloat() / 2
    private var aiming = false
    private var last = 0L
    private var cooldown = 0f
    private var flash = 0f
    private var scale = 1f
    private var ox = 0f
    private var oy = 0f
    private data class Arrow(var x: Float, var y: Float, val vx: Float, val vy: Float)
    private data class Spark(var x: Float, var y: Float, val vx: Float, val vy: Float, var life: Float)
    private val arrows = mutableListOf<Arrow>()
    private val sparks = mutableListOf<Spark>()
    private val stars = List(70) { val r = Random(it * 97); floatArrayOf(r.nextFloat()*400, r.nextFloat()*690, r.nextFloat()*1.5f+.5f) }
    init { isFocusable = true; contentDescription = "嫦娥射月亮：拖曳瞄準，放手射箭；四十秒內射中五次贏月餅。" }
    fun suspendGame() { if (screen == "play") paused = true; aiming = false; active = false; last = 0 }
    fun resumeFrames() { active = true; last = 0; invalidate() }
    private fun start() { screen = "play"; paused = false; time = 40f; hits = 0; phase = 0f; cooldown = 0f; arrows.clear(); sparks.clear(); last = 0; aiming = false }
    private fun finish(won: Boolean) {
        screen = if (won) "win" else "lose"; aiming = false
        if (won) { cakes++; level++; prefs.edit().putInt("cakes", cakes).putInt("level", level).apply() }
        arrows.clear()
    }
    private fun update(dt: Float) {
        if (screen != "play" || paused) return
        time = max(0f, time-dt); phase += dt * (0.8f + min(level, 12)*.12f)
        val oldMoonX = moonX; val oldMoonY = moonY
        moonX = 200 + sin(phase)*122; moonY = 245 + sin(phase*1.7f)*45
        cooldown = max(0f, cooldown-dt); flash = max(0f, flash-dt)
        val iter = arrows.iterator()
        while (iter.hasNext()) {
            val a = iter.next(); val px = a.x-oldMoonX; val py = a.y-oldMoonY
            a.x += a.vx*dt; a.y += a.vy*dt
            val qx = a.x-moonX; val qy = a.y-moonY
            val dx = qx-px; val dy = qy-py
            val t = (-(px*dx+py*dy)/(dx*dx+dy*dy).coerceAtLeast(.001f)).coerceIn(0f,1f)
            if (hypot(px+t*dx,py+t*dy) < 39f) {
                iter.remove(); hits++; flash=.25f
                repeat(18) { val r=Random.nextFloat()*6.283f; val v=Random.nextFloat()*110+35; sparks.add(Spark(moonX,moonY,cos(r)*v,sin(r)*v, .7f)) }
            } else if(a.y < -40 || a.x < -40 || a.x > 440) iter.remove()
        }
        sparks.forEach { it.x+=it.vx*dt; it.y+=it.vy*dt; it.life-=dt }; sparks.removeAll { it.life<=0 }
        if(hits>=5) finish(true) else if(time<=0) finish(false)
    }
    private fun color(c: Int) { ink.color=c; ink.style=Paint.Style.FILL; ink.shader=null }
    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, col: Int = white) { color(col); ink.textSize=size; ink.typeface=Typeface.create("sans-serif",Typeface.NORMAL); ink.textAlign=Paint.Align.CENTER; c.drawText(s,x,y,ink) }
    private fun line(c: Canvas,x: Float,y: Float,xx: Float,yy: Float,col: Int,w: Float=2f) { color(col); ink.strokeWidth=w; ink.strokeCap=Paint.Cap.ROUND; c.drawLine(x,y,xx,yy,ink) }
    private fun pill(c: Canvas,s: String,y: Float,filled: Boolean=true) { color(if(filled) gold else Color.rgb(42,49,78)); c.drawRoundRect(45f,y,355f,y+54,27f,27f,ink); text(c,s,200f,y+34,17f,if(filled) Color.rgb(34,32,52) else white) }
    private fun moon(c: Canvas,x: Float,y: Float,r: Float) {
        color(0x14F8D68E); c.drawCircle(x,y,r+19,ink); color(0x18F8D68E); c.drawCircle(x,y,r+9,ink)
        color(if(flash>0) Color.WHITE else gold); c.drawCircle(x,y,r,ink)
        color(Color.rgb(221,184,116)); c.drawCircle(x-r*.3f,y-r*.25f,r*.19f,ink); c.drawCircle(x+r*.33f,y+r*.3f,r*.25f,ink); c.drawCircle(x-r*.38f,y+r*.4f,r*.10f,ink)
    }
    private fun cake(c: Canvas,x: Float,y: Float,r: Float) {
        color(Color.rgb(174,103,48)); repeat(12) { val a=it*Math.PI/6; c.drawCircle(x+cos(a).toFloat()*r*.77f,y+sin(a).toFloat()*r*.77f,r*.29f,ink) }; c.drawCircle(x,y,r*.88f,ink)
        color(Color.rgb(236,174,86)); c.drawCircle(x,y,r*.83f,ink); ink.style=Paint.Style.STROKE; ink.strokeWidth=2f; ink.color=Color.rgb(154,86,42); c.drawCircle(x,y,r*.67f,ink); ink.style=Paint.Style.FILL
        text(c,"月",x,y+r*.19f,r*.60f,Color.rgb(143,75,38))
    }
    private fun heroine(c: Canvas) {
        // Flowing ribbons, jade dress, hair bun and a gold bow.
        color(0xFF9ABED0.toInt()); val ribbon=Path(); ribbon.moveTo(190f,670f); ribbon.cubicTo(100f,625f,140f,746f,64f,712f); ribbon.cubicTo(132f,778f,162f,659f,200f,697f); c.drawPath(ribbon,ink)
        color(0xFF91C5C3.toInt()); val dress=Path(); dress.moveTo(184f,663f); dress.lineTo(213f,663f); dress.cubicTo(214f,704f,240f,727f,248f,738f); dress.quadTo(198f,757f,153f,738f); dress.quadTo(179f,705f,184f,663f); c.drawPath(dress,ink)
        color(0xFFF1D2BC.toInt()); c.drawCircle(198f,644f,17f,ink)
        color(0xFF27283F.toInt()); c.drawArc(180f,624f,216f,660f,180f,180f,true,ink); c.drawCircle(199f,621f,10f,ink)
        line(c,189f,621f,214f,621f,gold,3f); line(c,185f,676f,225f,660f,0xFFF1D2BC.toInt(),8f)
        color(gold); ink.style=Paint.Style.STROKE; ink.strokeWidth=3f; c.drawArc(202f,628f,245f,695f,-80f,160f,false,ink); line(c,228f,629f,228f,694f,gold,1f)
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val now=System.nanoTime(); val dt=if(last==0L) 0f else ((now-last)/1e9f).coerceAtMost(.04f); last=now; update(dt)
        c.drawColor(0xFF10152F.toInt()); val availableW = width-paddingLeft-paddingRight; val availableH = height-paddingTop-paddingBottom
        scale=min(availableW/400f,availableH/800f).coerceAtLeast(.01f); ox=paddingLeft+(availableW-400*scale)/2; oy=paddingTop+(availableH-800*scale)/2
        c.save(); c.translate(ox,oy); c.scale(scale,scale)
        ink.shader=LinearGradient(0f,0f,0f,800f,intArrayOf(0xFF10152F.toInt(),0xFF252A50.toInt(),0xFF424767.toInt()),null,Shader.TileMode.CLAMP); c.drawRect(0f,0f,400f,800f,ink); ink.shader=null
        for(s in stars) { color(0x88EEE5D2.toInt()); c.drawCircle(s[0],s[1],s[2],ink) }
        color(0xFF303A59.toInt()); c.drawOval(-110f,695f,340f,1010f,ink); color(0xFF26334D.toInt()); c.drawOval(130f,710f,590f,960f,ink)
        if(screen=="play") {
            text(c,"第 $level 關",63f,47f,17f,gold); text(c,"月餅 $cakes",200f,47f,15f); text(c,"暫停 Ⅱ",338f,47f,16f)
            text(c,"${ceil(time).toInt()} 秒",75f,102f,27f); text(c,"射中 $hits / 5",305f,102f,23f,gold)
            color(0xFF424966.toInt()); c.drawRoundRect(35f,121f,365f,126f,3f,3f,ink); color(gold); c.drawRoundRect(35f,121f,35+330*time/40,126f,3f,3f,ink)
            moon(c,moonX,moonY,38f)
            if(aiming) repeat(12) { i -> color(0x80F4D290.toInt()); c.drawCircle(200+cos(angle)*i*25,650+sin(angle)*i*25,2f,ink) }
            arrows.forEach { a -> val norm=hypot(a.vx,a.vy); val dx=a.vx/norm; val dy=a.vy/norm; line(c,a.x-dx*27,a.y-dy*27,a.x,a.y,gold,3f); line(c,a.x,a.y,a.x-dx*9-dy*5,a.y-dy*9+dx*5,white); line(c,a.x,a.y,a.x-dx*9+dy*5,a.y-dy*9-dx*5,white) }
            sparks.forEach { color(gold); c.drawCircle(it.x,it.y,it.life*5,ink) }; heroine(c)
            text(c,if(aiming) "放手射箭！" else "拖曳瞄準 · 放手射箭",200f,782f,15f,muted)
            if(paused) { color(0xDB10152F.toInt()); c.drawRect(0f,0f,400f,800f,ink); text(c,"休息一下",200f,315f,32f,gold); text(c,"月亮等緊你",200f,354f,17f,muted); pill(c,"繼續遊戲",405f); pill(c,"返回主頁",480f,false) }
        } else {
            text(c,"MID-AUTUMN  ·  MOON QUEST",200f,58f,12f,gold)
            if(screen=="home") {
                moon(c,200f,213f,74f); text(c,"嫦娥射月亮",200f,348f,37f); text(c,"拉弓追月，贏取團圓滋味",200f,386f,17f,muted)
                text(c,"40 秒內射中移動月亮 5 次",200f,453f,18f); text(c,"拖曳瞄準方向，放手即射箭",200f,484f,16f,muted)
                pill(c,"開始追月  →  第 $level 關",539f); pill(c,"月餅收藏  ·  $cakes 個",610f,false); text(c,"原生離線小遊戲 · 中秋快樂",200f,759f,13f,muted)
            } else if(screen=="collection") {
                text(c,"團圓月餅盒",200f,155f,32f,gold); cake(c,200f,299f,76f); text(c,"已收集 $cakes 個月餅",200f,433f,24f); text(c,"每過一關，獲得一個蓮蓉月餅",200f,478f,16f,muted); text(c,"遊戲內虛擬獎勵",200f,510f,14f,muted); pill(c,"返回主頁",610f)
            } else {
                val won=screen=="win"; text(c,if(won) "追月成功！" else "差少少就得！",200f,165f,34f,gold)
                if(won) cake(c,200f,295f,80f) else moon(c,200f,290f,65f)
                text(c,if(won) "獲得蓮蓉月餅 × 1" else "今次射中 $hits / 5",200f,441f,24f)
                text(c,if(won) "月餅已放入收藏，共 $cakes 個" else "試下瞄準月亮前方，預判移動！",200f,484f,16f,muted)
                pill(c,if(won) "再追一輪  →  第 $level 關" else "再試一次",550f); pill(c,"返回主頁",621f,false)
            }
        }
        c.restore(); if(active && screen=="play" && !paused) postInvalidateOnAnimation()
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x=(e.x-ox)/scale; val y=(e.y-oy)/scale
        if(e.action==MotionEvent.ACTION_CANCEL) { aiming=false; invalidate(); return true }
        if(screen=="play" && !paused) {
            if(e.action==MotionEvent.ACTION_DOWN && x>290 && y<75) { paused=true; aiming=false; invalidate(); return true }
            if(e.action==MotionEvent.ACTION_DOWN || e.action==MotionEvent.ACTION_MOVE) { aiming=true; angle=atan2((y-650).coerceAtMost(-90f),x-200).coerceIn(-2.85f,-.29f) }
            if(e.action==MotionEvent.ACTION_UP && aiming) { if(cooldown<=0) { arrows.add(Arrow(200f,650f,cos(angle)*690,sin(angle)*690)); cooldown=.22f }; aiming=false; performClick() }
        } else if(e.action==MotionEvent.ACTION_UP && x in 45f..355f) {
            performClick()
            when(screen) {
                "home" -> if(y in 539f..593f) start() else if(y in 610f..664f) screen="collection"
                "collection" -> if(y in 610f..664f) screen="home"
                "play" -> if(y in 405f..459f) { paused=false; last=0 } else if(y in 480f..534f) screen="home"
                else -> if(y in 550f..604f) start() else if(y in 621f..675f) screen="home"
            }
        }
        invalidate(); return true
    }
}
