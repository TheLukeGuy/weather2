package sh.lpx.weather2;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import sh.lpx.weather2.entity.EntityLightningBolt;
import sh.lpx.weather2.volcano.Volcano;
import sh.lpx.weather2.util.WeatherUtilBlock;
import sh.lpx.weather2.weather.ServerWeatherManager;
import sh.lpx.weather2.weather.storm.CloudStorm;
import sh.lpx.weather2.weather.storm.SandStorm;
import sh.lpx.weather2.weather.storm.Storm;

import java.util.List;
import java.util.Random;

public class CommandWeather2 extends CommandBase {

    @Override
    public String getName() {
        return "sh/lpx/weather2";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender var1, String[] var2) {

        String helpMsgStorm = "Syntax: storm create <rain/thunder/wind/spout/hail/F0/F1/F2/F3/F4/F5/C0/C1/C2/C3/C4/C5/hurricane> <Optional: alwaysProgress>... example: storm create F1 alwaysProgress ... eg2: storm killall";

        PlayerEntity player = null;
        if (var1 instanceof PlayerEntity) {
            player = (PlayerEntity) var1;
        }
        World world = var1.getEntityWorld();
        int dimension = world.provider.getDimension();
        BlockPos posBlock = var1.getPosition();
        Vec3d posVec = var1.getPositionVector();

        try {
			/*if(var1 instanceof EntityPlayerMP)
			{*/
            //EntityPlayer player = getCommandSenderAsPlayer(var1);

            if (var2[0].equals("volcano")) {
                if (var2[1].equals("create") && posVec != Vec3d.ZERO) {
                    if (dimension == 0) {
                        ServerWeatherManager wm = ServerTickHandler.lookupDimToWeatherMan.get(0);
                        Volcano vo = new Volcano(wm);
                        vo.pos = new Vec3(posVec);
                        vo.initFirstTime();
                        wm.addVolcano(vo);
                        vo.initPost();

                        wm.syncVolcanoNew(vo);

                        sendCommandSenderMsg(var1, "volcano created");
                    } else {
                        sendCommandSenderMsg(var1, "can only make volcanos on main overworld");
                    }
                }
            } else if (var2[0].equals("testLightning")) {
                Random rand = new Random();
                EntityLightningBolt ent = new EntityLightningBolt(world, posBlock.getX() + rand.nextInt(2) - +rand.nextInt(2)
                        , posBlock.getY(), posBlock.getZ() + rand.nextInt(2) - +rand.nextInt(2));
                ServerWeatherManager wm = ServerTickHandler.lookupDimToWeatherMan.get(dimension);
                wm.getWorld().weatherEffects.add(ent);
                wm.syncLightningNew(ent, false);
                sendCommandSenderMsg(var1, "spawned lightning");
            } else if (var2[0].equals("storm")) {
                if (var2[1].equalsIgnoreCase("killAll")) {
                    ServerWeatherManager wm = ServerTickHandler.lookupDimToWeatherMan.get(dimension);
                    sendCommandSenderMsg(var1, "killing all storms");
                    List<Storm> listStorms = wm.getStorms();
                    for (int i = 0; i < listStorms.size(); i++) {
                        Storm wo = listStorms.get(i);
                        if (wo instanceof Storm) {
                            Storm so = wo;
                            Weather.dbg("force killing storm ID: " + so.id);
                            so.setDead();
                        }
                    }
                } else if (var2[1].equalsIgnoreCase("killDeadly")) {
                    ServerWeatherManager wm = ServerTickHandler.lookupDimToWeatherMan.get(dimension);
                    sendCommandSenderMsg(var1, "killing all deadly storms");
                    List<Storm> listStorms = wm.getStorms();
                    for (int i = 0; i < listStorms.size(); i++) {
                        Storm wo = listStorms.get(i);
                        if (wo instanceof CloudStorm) {
                            CloudStorm so = (CloudStorm) wo;
                            if (so.levelCurIntensityStage >= CloudStorm.STATE_THUNDER) {
                                Weather.dbg("force killing storm ID: " + so.id);
                                so.setDead();
                            }
                        }
                    }
                } else if (var2[1].equalsIgnoreCase("killRain") || var2[1].equalsIgnoreCase("killStorm")) {
                    ServerWeatherManager wm = ServerTickHandler.lookupDimToWeatherMan.get(dimension);
                    sendCommandSenderMsg(var1, "killing all raining or deadly storms");
                    List<Storm> listStorms = wm.getStorms();
                    for (int i = 0; i < listStorms.size(); i++) {
                        Storm wo = listStorms.get(i);
                        if (wo instanceof CloudStorm) {
                            CloudStorm so = (CloudStorm) wo;
                            if (so.levelCurIntensityStage >= CloudStorm.STATE_THUNDER || so.attrib_precipitation) {
                                Weather.dbg("force killing storm ID: " + so.id);
                                so.setDead();
                            }
                        }
                    }
                } else if (var2[1].equals("create") || var2[1].equals("spawn")) {
                    if (var2.length > 2 && posVec != Vec3d.ZERO) {
                        //TODO: make this handle non StormObject types better, currently makes instance and doesnt use that type if its a sandstorm
                        boolean spawnCloudStorm = true;
                        ServerWeatherManager wm = ServerTickHandler.lookupDimToWeatherMan.get(dimension);
                        CloudStorm so = new CloudStorm(wm);
                        so.layer = 0;
                        so.userSpawnedFor = player != null ? player.getEntityName() : "nullObject";
                        so.naturallySpawned = false;
                        so.levelTemperature = 0.1F;
                        so.pos = new Vec3(posVec.x, CloudStorm.layers.get(so.layer), posVec.z);

                        so.levelWater = so.levelWaterStartRaining * 2;
                        so.attrib_precipitation = true;

                        if (!var2[2].equals("rain")) {
                            so.initRealStorm(null, null);
                        }

                        if (var2[2].equals("rain")) {

                        } else if (var2[2].equalsIgnoreCase("thunder") || var2[2].equalsIgnoreCase("lightning")) {
                            so.levelCurIntensityStage = CloudStorm.STATE_THUNDER;
                        } else if (var2[2].equalsIgnoreCase("wind")) {
                            so.levelCurIntensityStage = CloudStorm.STATE_HIGHWIND;
                        } else if (var2[2].equalsIgnoreCase("spout")) {
                            so.levelCurIntensityStage = CloudStorm.STATE_HIGHWIND;
                            so.attrib_waterSpout = true;
                        } else if (var2[2].equalsIgnoreCase("hail")) {
                            so.levelCurIntensityStage = CloudStorm.STATE_HAIL;
                        } else if (var2[2].equalsIgnoreCase("F5")) {
                            so.levelCurIntensityStage = CloudStorm.STATE_STAGE5;
                        } else if (var2[2].equalsIgnoreCase("F4")) {
                            so.levelCurIntensityStage = CloudStorm.STATE_STAGE4;
                        } else if (var2[2].equalsIgnoreCase("F3")) {
                            so.levelCurIntensityStage = CloudStorm.STATE_STAGE3;
                        } else if (var2[2].equalsIgnoreCase("F2")) {
                            so.levelCurIntensityStage = CloudStorm.STATE_STAGE2;
                        } else if (var2[2].equalsIgnoreCase("F1")) {
                            so.levelCurIntensityStage = CloudStorm.STATE_STAGE1;
                        } else if (var2[2].equalsIgnoreCase("firenado")) {
                            so.levelCurIntensityStage = CloudStorm.STATE_STAGE1;
                            so.isFirenado = true;
                        } else if (var2[2].equalsIgnoreCase("F0")) {
                            so.levelCurIntensityStage = CloudStorm.STATE_FORMING;
                        } else if (var2[2].equalsIgnoreCase("C0")) {
                            so.stormType = CloudStorm.TYPE_WATER;
                            so.levelCurIntensityStage = CloudStorm.STATE_FORMING;
                        } else if (var2[2].equalsIgnoreCase("C1")) {
                            so.stormType = CloudStorm.TYPE_WATER;
                            so.levelCurIntensityStage = CloudStorm.STATE_STAGE1;
                        } else if (var2[2].equalsIgnoreCase("C2")) {
                            so.stormType = CloudStorm.TYPE_WATER;
                            so.levelCurIntensityStage = CloudStorm.STATE_STAGE2;
                        } else if (var2[2].equalsIgnoreCase("C3")) {
                            so.stormType = CloudStorm.TYPE_WATER;
                            so.levelCurIntensityStage = CloudStorm.STATE_STAGE3;
                        } else if (var2[2].equalsIgnoreCase("C4")) {
                            so.stormType = CloudStorm.TYPE_WATER;
                            so.levelCurIntensityStage = CloudStorm.STATE_STAGE4;
                        } else if (var2[2].equalsIgnoreCase("C5") || var2[2].equalsIgnoreCase("hurricane")) {
                            so.stormType = CloudStorm.TYPE_WATER;
                            so.levelCurIntensityStage = CloudStorm.STATE_STAGE5;
                        } else if (var2[2].equalsIgnoreCase("hurricane")) {
                            so.stormType = CloudStorm.TYPE_WATER;
                            so.levelCurIntensityStage = CloudStorm.STATE_STAGE5;
                        } else if (var2[2].equalsIgnoreCase("full")) {
                            //needs code to somehow guarantee it will build to max stage
                            so.levelCurIntensityStage = CloudStorm.STATE_THUNDER;
                            so.alwaysProgresses = true;
                        } else if (var2[2].equalsIgnoreCase("test")) {
                            so.levelCurIntensityStage = CloudStorm.STATE_THUNDER;
                        } else if (var2[2].equalsIgnoreCase("sandstormUpwind")) {

                            SandStorm sandstorm = new SandStorm(wm);

                            //sandstorm.pos = new Vec3(player.posX, player.world.getHeight(new BlockPos(player.posX, 0, player.posZ)).getY() + 1, player.posZ);

                            Vec3 pos = new Vec3(posVec.x, world.getHeight(new BlockPos(posVec.x, 0, posVec.z)).getY() + 1, posVec.z);


                            /**
                             * adjust position upwind 150 blocks
                             */
                            float angle = wm.getWindManager().getWindAngleForClouds();
                            double vecX = -Math.sin(Math.toRadians(angle));
                            double vecZ = Math.cos(Math.toRadians(angle));
                            double speed = 150D;
                            pos.xCoord -= vecX * speed;
                            pos.zCoord -= vecZ * speed;

                            sandstorm.initFirstTime();
                            sandstorm.initSandstormSpawn(pos);


                            wm.addStorm(sandstorm);
                            wm.syncStormNew(sandstorm);
                            spawnCloudStorm = false;

                            wm.windManager.startHighWindEvent();
                            wm.windManager.lowWindTimer = 0;

                        } else if (var2[2].equalsIgnoreCase("sandstorm")) {
                            boolean spawned = wm.trySpawnSandstormNearPos(world, new Vec3(posVec));
                            spawnCloudStorm = false;
                            if (!spawned) {
                                sendCommandSenderMsg(var1, "couldnt find spot to spawn");
                                return;
                            } else {
                                wm.windManager.startHighWindEvent();
                                wm.windManager.lowWindTimer = 0;
                            }
                        }

                        if (var2.length > 3) {
                            if (var2[3].contains("Progress") || var2[3].contains("progress")) {
                                so.alwaysProgresses = true;
                            }
                        }

                        if (spawnCloudStorm) {
                            so.initFirstTime();

                            //lock it to current stage or less
                            so.levelStormIntensityMax = so.levelCurIntensityStage;

                            wm.addStorm(so);
                            wm.syncStormNew(so);
                        }

                        sendCommandSenderMsg(var1, "storm " + var2[2] + " created" + (so.alwaysProgresses ? ", flags: alwaysProgresses" : ""));
                    } else {
                        sendCommandSenderMsg(var1, helpMsgStorm);
                    }
                } else if (var2[1].equals("help")) {
                    sendCommandSenderMsg(var1, helpMsgStorm);

                } else {
                    sendCommandSenderMsg(var1, helpMsgStorm);
                }
            } else if (var2[0].equals("testderp") && player != null) {
                //EntityPlayerMP player = var1;
                WeatherUtilBlock.floodAreaWithLayerableBlock(player.world, new Vec3(player.posX, player.posY, player.posZ), player.rotationYawHead, 1, 1, CommonProxy.blockSandLayer, 30);
            } else if (var2[0].equals("wind")) {
                if (var2[1].equals("high")) {
                    boolean doHighOn = false;
                    boolean doHighOff = false;
                    if (var2.length > 2) {
                        if (var2[2].equals("start")) {
                            doHighOn = true;
                        } else if (var2[2].equals("stop")) {
                            doHighOff = true;
                        }
                    } else {
                        doHighOn = true;
                    }
                    ServerWeatherManager wm = ServerTickHandler.getWeatherSystemForDim(dimension);
                    if (doHighOn) {
                        wm.windManager.startHighWindEvent();
                        //cancel any low wind state if there is one
                        wm.windManager.lowWindTimer = 0;
                        sendCommandSenderMsg(var1, "started high wind event");
                    } else if (doHighOff) {
                        wm.windManager.stopHighWindEvent();
                        sendCommandSenderMsg(var1, "stopped high wind event");
                    }
                } else if (var2[1].equals("low")) {
                    boolean doLowOn = false;
                    boolean doLowOff = false;
                    if (var2.length > 2) {
                        if (var2[2].equals("start")) {
                            doLowOn = true;
                        } else if (var2[2].equals("stop")) {
                            doLowOff = true;
                        }
                    } else {
                        doLowOn = true;
                    }
                    ServerWeatherManager wm = ServerTickHandler.getWeatherSystemForDim(dimension);
                    if (doLowOn) {
                        wm.windManager.startLowWindEvent();
                        //cancel any high wind state if there is one
                        wm.windManager.highWindTimer = 0;
                        sendCommandSenderMsg(var1, "started low wind event");
                    } else if (doLowOff) {
                        wm.windManager.stopLowWindEvent();
                        sendCommandSenderMsg(var1, "stopped low wind event");
                    }
                }
            } else {
                sendCommandSenderMsg(var1, helpMsgStorm);
            }
            /*}*/
        } catch (Exception ex) {
            System.out.println("Exception handling Weather2 command");
            sendCommandSenderMsg(var1, helpMsgStorm);
            ex.printStackTrace();
        }

    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender par1ICommandSender) {
        return par1ICommandSender.canUseCommand(this.getRequiredPermissionLevel(), this.getName());
    }

    @Override
    public String getUsage(ICommandSender icommandsender) {
        return "Magic dev method!";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    public static void sendCommandSenderMsg(ICommandSender entP, String msg) {
        entP.sendMessage(new TextComponentString(msg));
    }

}
