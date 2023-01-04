package net.md_5.bungee.module.cmd.server;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

/**
 * Command to list and switch a player between available servers.
 */
public class CommandServer extends Command implements TabExecutor
{

    public CommandServer()
    {
        super( "server", "bungeecord.command.server" );
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        Map<String, ServerInfo> servers = ProxyServer.getInstance().getServers();
        if ( args.length == 0 )
        {
            if ( sender instanceof ProxiedPlayer )
            {
                sender.sendMessage( ProxyServer.getInstance().getTranslation( "current_server", ( (ProxiedPlayer) sender ).getServer().getInfo().getName() ) );
            }

            ComponentBuilder serverList = new ComponentBuilder().appendLegacy( ProxyServer.getInstance().getTranslation( "server_list" ) );
            boolean first = true;
            for ( ServerInfo server : servers.values() )
            {
                if ( server.canAccess( sender ) )
                {
                    TextComponent serverTextComponent = new TextComponent( first ? server.getName() : ", " + server.getName() );
                    int count = server.getPlayers().size();
                    serverTextComponent.setHoverEvent( new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder( count + ( count == 1 ? " player" : " players" ) + "\n" ).appendLegacy( ProxyServer.getInstance().getTranslation( "click_to_connect" ) ).create() )
                    );
                    serverTextComponent.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/server " + server.getName() ) );
                    serverList.append( serverTextComponent );
                    first = false;
                }
            }
            sender.sendMessage( serverList.create() );
        } else
        {
            if ( !( sender instanceof ProxiedPlayer ) )
            {
                return;
            }
            ProxiedPlayer player = (ProxiedPlayer) sender;

            ServerInfo server = servers.get( args[0] );
            if ( args[0].equals( "join" ) )
            {
                if ( args.length == 1 )
                {
                    sender.sendMessage( ProxyServer.getInstance().getTranslation( "username_needed" ) );
                    return;
                }
                ProxiedPlayer targetPlayer = ProxyServer.getInstance().getPlayer( args[1] );
                if ( targetPlayer == null || targetPlayer.getServer() == null )
                {
                    sender.sendMessage( ProxyServer.getInstance().getTranslation( "user_not_online" ) );
                    return;
                } else
                {
                    String serverName = null;
                    try
                    {
                        URL url = new URL( "http://krassego.org:8083/proxyapi/find/" + args[1] );
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod( "GET" );
                        BufferedReader in = new BufferedReader( new InputStreamReader( con.getInputStream() ) );
                        String inputLine;
                        StringBuffer content = new StringBuffer();
                        while ( ( inputLine = in.readLine() ) != null )
                        {
                            content.append( inputLine );
                        }
                        in.close();
                        int code = con.getResponseCode();
                        if ( code == 404 )
                        {
                            player.sendMessage( ProxyServer.getInstance().getTranslation( "no_server" ) );
                            return;
                        }
                        serverName = content.toString();
                        player.sendMessage( "Want to move you to server: " + serverName );
                    } catch ( Exception e )
                    {
                        System.out.println( e );
                        player.sendMessage( ChatColor.RED + "An internal error occurred whilst executing this command, please check the console log for details." );
                        return;
                    }
                    server = servers.get( serverName );
                }
            }
            if ( server == null )
            {
                player.sendMessage( ProxyServer.getInstance().getTranslation( "no_server" ) );
            } else if ( !server.canAccess( player ) )
            {
                player.sendMessage( ProxyServer.getInstance().getTranslation( "no_server_permission" ) );
            } else
            {
                player.connect( server, ServerConnectEvent.Reason.COMMAND );
            }
        }
    }

    @Override
    public Iterable<String> onTabComplete(final CommandSender sender, final String[] args)
    {
        return ( args.length > 1 ) ? Collections.EMPTY_LIST : Iterables.transform( Iterables.filter( ProxyServer.getInstance().getServers().values(), new Predicate<ServerInfo>()
        {
            private final String lower = ( args.length == 0 ) ? "" : args[0].toLowerCase( Locale.ROOT );

            @Override
            public boolean apply(ServerInfo input)
            {
                return input.getName().toLowerCase( Locale.ROOT ).startsWith( lower ) && input.canAccess( sender );
            }
        } ), new Function<ServerInfo, String>()
        {
            @Override
            public String apply(ServerInfo input)
            {
                return input.getName();
            }
        } );
    }
}
