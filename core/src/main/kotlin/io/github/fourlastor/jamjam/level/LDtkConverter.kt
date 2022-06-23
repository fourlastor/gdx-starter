package io.github.fourlastor.jamjam.level

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Rectangle
import io.github.fourlastor.ldtk.Definitions
import io.github.fourlastor.ldtk.LDtkEntityInstance
import io.github.fourlastor.ldtk.LDtkLayerInstance
import io.github.fourlastor.ldtk.LDtkLevelDefinition
import io.github.fourlastor.ldtk.LDtkTileInstance

class LDtkConverter(private val scale: Float) {

    private val atlas by lazy { TextureAtlas(Gdx.files.internal("tiles.atlas")) }

    fun convert(levelDefinition: LDtkLevelDefinition, definitions: Definitions): Level {
        val layerInstances = levelDefinition.layerInstances.orEmpty().reversed()
        return Level(
            statics = LevelStatics(
                spriteLayers = layerInstances
                    .mapNotNull { it.toLayer(definitions) },
                staticBodies = layerInstances.firstOrNull { it.type == "IntGrid" }
                    .toBoxes()
            ),
            player = layerInstances
                .firstOrNull { it.type == "Entities" }
                .let { checkNotNull(it) { "Entities layer missing from level." } }
                .let { layer ->
                    layer.entityInstances
                        .firstOrNull { it.identifier == "Player" }
                        .let { checkNotNull(it) { "Player missing from entity layer." } }
                        .let {
                            Player(
                                atlas = atlas,
                                dimensions = Rectangle(it.x * scale, it.y * scale, 16f * scale, 16f * scale)
                            )
                        }
                }


        )
    }

    /** Converts an IntGrid layer to definitions used in the physics world. */
    private fun LDtkLayerInstance?.toBoxes(): List<Rectangle> = this?.run {

        fun Int.x() = (this % cWid).toFloat()
        fun Int.y() = (this / cWid).toFloat()

        intGridCSV.orEmpty()
            .mapIndexedNotNull { index, i ->
                index.takeIf { i == 1 }?.let {
                    Rectangle(
                        index.x(),
                        index.y(),
                        gridSize * scale,
                        gridSize * scale,
                    )
                }
            }
    }.orEmpty()

    private fun LDtkLayerInstance.toLayer(definitions: Definitions): SpriteLayer? =
        when (type) {
            "AutoLayer" -> {
                definitions.tilesets.find { it.uid == tilesetDefUid }
                    ?.let { tileset ->
                        SpriteLayer(
                            atlas,
                            autoLayerTiles.mapNotNull { tile ->
                                tile.t
                                    .let { tileId -> tileset.customData.find { it.tileId == tileId } }
                                    ?.let { atlas.createSprite(it.data) }
                                    ?.apply {
                                        setOrigin(0f, 0f)
                                        setScale(scale)
                                        setPosition(tile.x * scale, tile.y * scale)

                                        val flipFlags = tile.f
                                        val x = flipFlags and 1 == 1
                                        val y = flipFlags shr 1 and 1 == 1
                                        flip(x, !y)
                                    }
                            })
                    }
            }

            else -> null
        }
}

private val LDtkTileInstance.x: Float
    get() = px[0].toFloat()
private val LDtkTileInstance.y: Float
    get() = px[1].toFloat()
private val LDtkEntityInstance.x: Float
    get() = px[0].toFloat()
private val LDtkEntityInstance.y: Float
    get() = px[1].toFloat()
