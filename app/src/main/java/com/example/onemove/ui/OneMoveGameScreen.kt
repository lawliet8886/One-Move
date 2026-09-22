package com.example.onemove.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onemove.model.*
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.render.ToyBoxRenderer

private val Ink = Color(0xFF0A1122)
private val Mint = Color(0xFF6EE7B7)
private val Muted = Color(0xFFA9BDD6)

@Composable
fun OneMoveGameScreen(viewModel: OneMoveViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    OneMoveGameContent(state,viewModel.physicsWorld,viewModel::pullPin,viewModel::resetGame,
        viewModel::nextLevel,viewModel::previousLevel,viewModel::openLevelSelect,viewModel::closeLevelSelect,
        viewModel::loadLevel,viewModel::onCanvasTap)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OneMoveGameContent(
    uiState: GameUiState, world: PhysicsWorld,
    onPinTapped: (PinId)->Unit = {}, onReset: ()->Unit = {}, onNextLevel: ()->Unit = {},
    onPrevLevel: ()->Unit = {}, onOpenLevelSelect: ()->Unit = {}, onCloseLevelSelect: ()->Unit = {},
    onSelectLevel: (Int)->Unit = {}, onCanvasTapCoordinates: (Float,Float)->Unit = {_,_->},
    animatePill: Boolean = true, enableGestures: Boolean = true
) {
    val frame = uiState.frameTick
    val rescued = uiState.creatures.count { it.isInsideGoal }
    BackHandler(enabled=uiState.showLevelSelectSheet,onBack=onCloseLevelSelect)
    Scaffold(containerColor=Ink,modifier=Modifier.fillMaxSize().semantics { testTagsAsResourceId=true }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.TopCenter) {
            Column(Modifier.widthIn(max=560.dp).fillMaxSize()) {
                Column(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=8.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text("ONE MOVE",color=Mint,fontSize=12.sp,letterSpacing=3.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f))
                        IconButton(onClick=onOpenLevelSelect,modifier=Modifier.size(48.dp).testTag("level_select_button")) {
                            Icon(Icons.Default.List,"Escolher fase",tint=Muted)
                        }
                        IconButton(onClick=onReset,modifier=Modifier.size(48.dp).testTag("reset_button")) {
                            Icon(Icons.Default.Refresh,"Recomeçar a fase",tint=Color(0xFFFCD34D))
                        }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Surface(color=Color(0xFF17354A),shape=RoundedCornerShape(14.dp)) {
                            Text("%02d".format(uiState.currentLevelNumber),Modifier.padding(12.dp),color=Mint,fontSize=24.sp,fontWeight=FontWeight.Black)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(uiState.currentLevel.name,color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                            Text("FASE ${uiState.currentLevelNumber} DE ${uiState.totalLevels}",color=Muted,fontSize=10.sp,letterSpacing=1.5.sp)
                        }
                        Column(horizontalAlignment=Alignment.End) {
                            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                repeat(3) { index -> Box(Modifier.size(9.dp).background(if(index<rescued) Mint else Color(0xFF34445D),CircleShape)) }
                            }
                            Text("$rescued/3 em casa",color=Muted,fontSize=10.sp,modifier=Modifier.padding(top=5.dp).testTag("rescue_counter"))
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),contentAlignment=Alignment.Center) {
                    BoxWithConstraints(Modifier.aspectRatio(LevelDefinition.WORLD_WIDTH/LevelDefinition.WORLD_HEIGHT).clip(RoundedCornerShape(22.dp)).border(1.dp,Color(0xFF304660),RoundedCornerShape(22.dp))) {
                        val scaleFactor = constraints.maxWidth.toFloat()/LevelDefinition.WORLD_WIDTH
                        Canvas(Modifier.fillMaxSize().testTag("game_board_canvas").semantics {
                            contentDescription="Tabuleiro do One Move"
                            stateDescription="fase=${uiState.currentLevelNumber};estado=${uiState.simulationState};resgatados=$rescued"
                        }.then(if(enableGestures) Modifier.pointerInput(uiState.simulationState,scaleFactor) {
                            detectTapGestures { tap -> onCanvasTapCoordinates(tap.x/scaleFactor,tap.y/scaleFactor) }
                        } else Modifier)) {
                            if(frame>=0) scale(scaleFactor,pivot=Offset.Zero) { ToyBoxRenderer.renderToyBox(this,world) }
                        }
                    }
                }
                // Results have their own reserved area and never cover the moving characters.
                Box(Modifier.fillMaxWidth().heightIn(min=150.dp).padding(horizontal=18.dp,vertical=8.dp)) {
                    if(uiState.showResultOverlay) ResultPanel(uiState,onReset,onNextLevel)
                    else Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(if(uiState.simulationState==SimulationState.READY) "UM MOVIMENTO. SALVE OS TRÊS." else "Acompanhe o caminho dos amigos…",color=Mint,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp,modifier=Modifier.testTag("move_counter_pill"))
                        Text(uiState.currentLevel.primaryMechanics,color=Muted,fontSize=12.sp,lineHeight=16.sp,textAlign=TextAlign.Center,maxLines=2,modifier=Modifier.fillMaxWidth().padding(vertical=7.dp))
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()) {
                            for(pin in world.pins) {
                                Button(onClick={onPinTapped(pin.id)},enabled=enableGestures && uiState.simulationState==SimulationState.READY,
                                    shape=RoundedCornerShape(16.dp),contentPadding=PaddingValues(0.dp),
                                    colors=ButtonDefaults.buttonColors(containerColor=pin.color,contentColor=Ink),
                                    modifier=Modifier.weight(1f).height(52.dp).testTag("pin_${pin.name}_button").semantics { contentDescription="Puxar pino ${pin.name}" }) {
                                    Text(pin.name,fontSize=22.sp,fontWeight=FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
            if(uiState.showLevelSelectSheet) LevelPicker(uiState,onSelectLevel,onCloseLevelSelect)
        }
    }
}

@Composable
private fun ResultPanel(ui:GameUiState,onReset:()->Unit,onNext:()->Unit) {
    val won=ui.simulationState==SimulationState.SUCCESS
    val final=won && ui.currentLevelNumber==ui.totalLevels
    Column(Modifier.fillMaxWidth().testTag(if(won) "success_overlay" else "failure_overlay"),horizontalAlignment=Alignment.CenterHorizontally) {
        Text(if(final) "TODOS EM CASA!" else if(won) "O trio chegou!" else "Vamos tentar outro caminho?",color=if(won) Mint else Color(0xFFFCD34D),fontSize=20.sp,fontWeight=FontWeight.Bold)
        Text(if(final) "Você completou as 12 fases. Que equipe!" else if(won) "Três amigos seguros. Um movimento certeiro." else when(ui.failureReason) {
            "PATH_BLOCKED" -> "Essa trava deixou o caminho fechado."
            "CREATURE_TRAPPED_IN_DANGER_BASIN" -> "Um amigo encostou na área de perigo."
            "CREATURE_OUT_OF_BOUNDS" -> "Um amigo saiu do tabuleiro."
            "TIME_LIMIT_REACHED" -> "O movimento não chegou ao refúgio a tempo."
            else -> "Observe as rampas e experimente outra trava."
        },color=Muted,fontSize=12.sp,textAlign=TextAlign.Center,modifier=Modifier.padding(vertical=7.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            if(won) OutlinedButton(onClick=onReset,modifier=Modifier.height(50.dp).testTag("replay_button"),shape=RoundedCornerShape(15.dp)) { Text("Repetir",color=Muted) }
            Button(onClick=if(won) onNext else onReset,modifier=Modifier.weight(1f).height(50.dp).testTag(if(won) "next_level_button" else "retry_button"),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=if(won) Mint else Color(0xFFFCD34D),contentColor=Ink)) {
                Text(if(final) "Ver minhas fases" else if(won) "Próxima aventura" else "Tentar de novo",fontWeight=FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LevelPicker(ui:GameUiState,onSelect:(Int)->Unit,onClose:()->Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xEE050B16)),contentAlignment=Alignment.Center) {
        Surface(color=Color(0xFF132237),shape=RoundedCornerShape(24.dp),modifier=Modifier.padding(18.dp).widthIn(max=480.dp).fillMaxWidth().testTag("level_picker")) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Suas aventuras",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold)
                        Text("${ui.completedLevels.size} de ${ui.totalLevels} refúgios alcançados",color=Muted,fontSize=12.sp)
                    }
                    IconButton(onClick=onClose,modifier=Modifier.testTag("close_level_picker")) { Icon(Icons.Default.Close,"Fechar fases",tint=Muted) }
                }
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(columns=GridCells.Fixed(3),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.heightIn(max=400.dp)) {
                    items(LevelCatalog.ALL_LEVELS,key={it.number}) { level ->
                        val unlocked=level.number<=ui.highestUnlockedLevel
                        val complete=level.number in ui.completedLevels
                        val selected=level.number==ui.currentLevelNumber
                        Column(Modifier.height(76.dp).clip(RoundedCornerShape(16.dp)).background(if(complete) Color(0xFF164B41) else Color(0xFF1B3049)).border(if(selected) 2.dp else 1.dp,if(selected) Mint else Color(0xFF35506B),RoundedCornerShape(16.dp)).testTag("level_card_${level.number}").clickable(enabled=unlocked){onSelect(level.number);onClose()},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                            Text("%02d".format(level.number),color=if(unlocked) Color.White else Muted.copy(alpha=0.45f),fontSize=23.sp,fontWeight=FontWeight.Bold)
                            Text(if(complete) "EM CASA" else if(unlocked) "JOGAR" else "FECHADA",color=if(complete) Mint else Muted,fontSize=9.sp,letterSpacing=0.7.sp)
                        }
                    }
                }
            }
        }
    }
}
