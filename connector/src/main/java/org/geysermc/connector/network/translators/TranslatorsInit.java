/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.translators;

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTOutputStream;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.data.GamePublishSetting;
import com.nukkitx.protocol.bedrock.data.GameRule;
import com.nukkitx.protocol.bedrock.packet.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.geysermc.connector.utils.GeyserUtils;
import org.geysermc.connector.utils.Toolbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class TranslatorsInit {
    private static final CompoundTag EMPTY_TAG = CompoundTagBuilder.builder().buildRootTag();
    private static final byte[] EMPTY_LEVEL_CHUNK_DATA;

    static {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write(new byte[258]); // Biomes + Border Size + Extra Data Size

            try (NBTOutputStream stream = NbtUtils.createNetworkWriter(outputStream)) {
                stream.write(EMPTY_TAG);
            }

            EMPTY_LEVEL_CHUNK_DATA = outputStream.toByteArray();
        }catch (IOException e) {
            throw new AssertionError("Unable to generate empty level chunk data");
        }
    }

    public static void start() {
        addLoginPackets();
    }

    private static void addLoginPackets() {
        Registry.add(ServerJoinGamePacket.class, (packet, session) -> {
            AdventureSettingsPacket bedrockPacket = new AdventureSettingsPacket();

            bedrockPacket.setUniqueEntityId(packet.getEntityId());

            session.getUpstream().sendPacketImmediately(bedrockPacket);

            StartGamePacket startGamePacket = new StartGamePacket();
            startGamePacket.setUniqueEntityId(packet.getEntityId());
            startGamePacket.setRuntimeEntityId(packet.getEntityId());
            startGamePacket.setPlayerGamemode(packet.getGameMode().ordinal());
            startGamePacket.setPlayerPosition(new Vector3f(0, 0, 0));
            startGamePacket.setRotation(new Vector2f(1, 1));

            startGamePacket.setSeed(1111);
            startGamePacket.setDimensionId(0);
            startGamePacket.setGeneratorId(0);
            startGamePacket.setLevelGamemode(packet.getGameMode().ordinal());
            startGamePacket.setDifficulty(1);
            startGamePacket.setDefaultSpawn(new Vector3i(0, 0, 0));
            startGamePacket.setAcheivementsDisabled(true);
            startGamePacket.setTime(0);
            startGamePacket.setEduLevel(false);
            startGamePacket.setEduFeaturesEnabled(false);
            startGamePacket.setRainLevel(0);
            startGamePacket.setLightningLevel(0);
            startGamePacket.setMultiplayerGame(true);
            startGamePacket.setBroadcastingToLan(true);
            startGamePacket.getGamerules().add(new GameRule<>("showcoordinates", true));
            startGamePacket.setPlatformBroadcastMode(GamePublishSetting.PUBLIC);
            startGamePacket.setXblBroadcastMode(GamePublishSetting.PUBLIC);
            startGamePacket.setCommandsEnabled(true);
            startGamePacket.setTexturePacksRequired(false);
            startGamePacket.setBonusChestEnabled(false);
            startGamePacket.setStartingWithMap(false);
            startGamePacket.setTrustingPlayers(true);
            startGamePacket.setDefaultPlayerPermission(1);
            startGamePacket.setServerChunkTickRange(4);
            startGamePacket.setBehaviorPackLocked(false);
            startGamePacket.setResourcePackLocked(false);
            startGamePacket.setFromLockedWorldTemplate(false);
            startGamePacket.setUsingMsaGamertagsOnly(false);
            startGamePacket.setFromWorldTemplate(false);
            startGamePacket.setWorldTemplateOptionLocked(false);

            startGamePacket.setLevelId("oerjhii");
            startGamePacket.setWorldName("world");
            startGamePacket.setPremiumWorldTemplateId("00000000-0000-0000-0000-000000000000");
            startGamePacket.setCurrentTick(0);
            startGamePacket.setEnchantmentSeed(0);
            startGamePacket.setMultiplayerCorrelationId("");
            startGamePacket.setCachedPalette(Toolbox.CACHED_PALLETE);
            startGamePacket.setItemEntries(Toolbox.ITEMS);

            session.getUpstream().sendPacket(startGamePacket);

            Vector3f pos = new Vector3f(0, 0, 0);

            int chunkX = pos.getFloorX() >> 4;

            int chunkZ = pos.getFloorZ() >> 4;

            for (int x = -3; x < 3; x++) {

                for (int z = -3; z < 3; z++) {

                    LevelChunkPacket data = new LevelChunkPacket();
                    data.setChunkX(chunkX + x);
                    data.setChunkZ(chunkZ + z);
                    data.setSubChunksLength(0);

                    data.setData(EMPTY_LEVEL_CHUNK_DATA);

                    session.getUpstream().sendPacketImmediately(data);

                }

            }

            PlayStatusPacket packet1 = new PlayStatusPacket();

            packet1.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);

            session.getUpstream().sendPacket(packet1);
        });
    }

    private static byte[] empty(byte[] b, Vector2i pos) {
        ByteBuf by = Unpooled.buffer();

        GeyserUtils.writePEChunkCoord(by, pos);

        return by.array();
    }

    private static class CanWriteToBB extends ByteArrayOutputStream {

        CanWriteToBB() {
            super(8192);
        }

        void writeTo(ByteBuf buf) {
            buf.writeBytes(super.buf, 0, super.count);
        }
    }
}
