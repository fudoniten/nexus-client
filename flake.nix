{
  description = "Nexus Client";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-23.11";
    utils = {
      url = "github:numtide/flake-utils";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    fudo-clojure = {
      url = "git+https://fudo.dev/public/fudo-clojure.git";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    nexus = {
      crypto.url = "git+https://fudo.dev/public/nexus-crypto.git";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    helpers = {
      url = "git+https://fudo.dev/public/nix-helpers.git";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, utils, helpers, fudo-clojure, nexus-crypto, ... }:
    utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };

        cljLibs = {
          "org.fudo/fudo-clojure" =
            fudo-clojure.packages."${system}".fudo-clojure;
          "org.fudo/nexus.crypto" =
            nexus-crypto.packages."${system}".nexus-crypto;
        };

      in {
        packages = rec {
          default = nexus-client;
          nexus-client = helpers.packages."${system}".mkClojureBin {
            name = "org.fudo/nexus-client";
            primaryNamespace = "nexus.client.cli";
            src = ./.;
            inherit cljLibs;
          };
        };

        devShells = rec {
          default = updateDeps;
          updateDeps = pkgs.mkShell {
            buildInputs = with helpers.packages."${system}";
              [ (updateClojureDeps cljLibs) ];
          };
        };
      });
}
