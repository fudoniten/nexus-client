packages:

{ config, lib, pkgs, ... }:

with lib;
let
  nexus-client = packages."${pkgs.system}".nexus-client;
  cfg = config.nexus.client;
in {
  imports = [ ../options.nix ];

  config = mkIf cfg.enable {
    systemd = {
      services = {
        nexus-client = let
          sshKeys = listToAttrs
            (map (key: nameValuePair (baseNameOf key.path) key.path)
              config.services.openssh.hostKeys);
        in {
          path = [ nexus-client pkgs.openssh ];
          serviceConfig = {
            DynamicUser = true;
            RuntimeDirectory = "nexus-client";
            LoadCredentials = [ "hmac.key:${cfg.hmac-key-file}" ]
              ++ (mapAttrsToList (filename: path: "${filename}:${path}")
                sshKeys);
            ExecStartPre = let
              cmds = map (filename:
                "ssh-keygen -r -f $CREDENTIALS_DIRECTORY/${filename} > $CACHE_DIRECTORY/${filename}.fp")
                (attrNames sshKeys);
            in pkgs.writeShellScript "generate-sshfps.sh" "\n";
            ExecStart = pkgs.writeShellScript "nexus-client.sh"
              (concatStringsSep " " ([
                "nexus-client"
                "--server=${config.nexus.server.hostname}"
                "--port=${toString config.nexus.server.port}"
                "--delay-seconds=${toString cfg.delay-seconds}"
                "--hostname=${cfg.hostname}"
                "--key-file=$CREDENTIALS_DIRECTORY/hmac.key"
              ] ++ (map (dom: "--domain=${dom}") cfg.domains)
                ++ (optional cfg.ipv4 "--ipv4") ++ (optional cfg.ipv6 "--ipv6")
                ++ (optionals cfg.sshfps
                  (map (filename: "--sshfp=${filename}.sp")
                    (attrNames sshKeys)))));
          };
        };
      };
    };
  };
}
