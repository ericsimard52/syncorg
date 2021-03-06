package com.coste.syncorg.synchronizers;

import android.content.Context;
import android.widget.Toast;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Session;

import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.util.FS;

import java.io.IOException;

public class SshSessionFactory extends JschConfigSessionFactory {
    Context context;
    ConnectionType connection;

    SshSessionFactory(Context context) {
        this.context = context;
        this.connection = getConnectionType(context);
    }

    /**
     * Determine if connection if HTTP, SSH+PubKey, SSH+Password
     * based on the provided path and whether or not the user has provided a PubKey
     *
     * @param context
     * @return
     */
    public static ConnectionType getConnectionType(Context context) {
        final AuthData authData = AuthData.getInstance(context);
        if (authData.getHost().startsWith("http")) return ConnectionType.kHttp;
        else {
            if (authData.usePassword()) return ConnectionType.kSshPubKey;
            else return ConnectionType.kSshPassword;
        }
    }

    public static String generateKeyPair(Context context) {
        JSch jsch = new JSch();

        KeyPair kpair = null;
        try {
            kpair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
            kpair.writePrivateKey(context.getFilesDir().getAbsoluteFile() + "/" + AuthData.PRIVATE_KEY);
            kpair.writePublicKey(context.getFilesDir().getAbsoluteFile() + "/" + AuthData.PUBLIC_KEY, "SyncOrgPubKey");
        } catch (JSchException | IOException e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
            return "";
        }

        return AuthData.getPublicKey(context);
    }

    @Override
    protected void configure(OpenSshConfig.Host host, Session session) {
        session.setPort(AuthData.getInstance(context).getPort());
        if (connection == ConnectionType.kSshPassword) {
            session.setPassword(AuthData.getInstance(context).getPassword());
        }

    }

    @Override
    protected JSch createDefaultJSch(FS fs) throws JSchException {
        JSch defaultJSch = super.createDefaultJSch(fs);

        if (connection == ConnectionType.kSshPubKey)
            defaultJSch.addIdentity(AuthData.getPrivateKeyPath(context));


        return defaultJSch;
    }

    enum ConnectionType {
        kHttp,
        kSshPubKey,
        kSshPassword
    }
}
