{
  description = "Nexus Client";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-23.11";
    utils.url = "github:numtide/flake-utils";
    helpers = {
      url = "git+https://fudo.dev/public/nix-helpers.git";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, utils, helpers, ... }:
    utils.lib.eachDefaultSystem (system:
      let pkgs = import nixpkgs { inherit system; };
      in {
        packages = rec {
          default = nexus-client;
          nexus-client = helpers.packages."${system}".mkClojureBin {
            name = "org.fudo/nexus-client";
            primaryNamespace = "nexus.client.cli";
            src = ./.;
          };
        };

        devShells = rec {
          default = update-deps;
          update-deps = pkgs.mkShell {
            buildInputs = with helpers.packages."${system}";
              [ updateClojureDeps ];
          };
        };
      });
}
