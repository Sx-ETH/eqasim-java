package org.eqasim.ile_de_france.drt.utils;

import org.apache.commons.io.FileUtils;
import org.eqasim.ile_de_france.drt.IDFDrtConfigGroup;
import org.eqasim.ile_de_france.drt.IDFDrtConfigurator;
import org.eqasim.core.components.drt.mode_choice.utilities.drt_rejection_penalty.DrtRejectionPenaltyProviderConfigGroup;
import org.eqasim.core.components.drt.mode_choice.utilities.drt_rejection_penalty.DrtRejectionsLinearPenaltyProviderConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigGenerator {

    public interface ConfigGenerationInterface {
        String next(Config baseConfig);
        boolean hasNext();
    }


    public static class DrtRejectionLinearPenaltyProviderSensitivityGenerator implements ConfigGenerationInterface {

        private double[] alphaValues;
        private double[] initialPenaltyValues;
        private double[] targetPenaltyValues;

        private int[] index = new int[]{0, 0, 0};

        public DrtRejectionLinearPenaltyProviderSensitivityGenerator(double[] initialPenaltyValues, double[] targetPenaltyValues, double[] alphaValues) {
            this.initialPenaltyValues = initialPenaltyValues;
            this.targetPenaltyValues = targetPenaltyValues;
            this.alphaValues = alphaValues;
        }


        @Override
        public String next(Config baseConfig) {
            assert this.hasNext();
            this.increaseIndex();
            IDFDrtConfigGroup idfDrtConfigGroup = (IDFDrtConfigGroup) baseConfig.getModules().get(IDFDrtConfigGroup.GROUP_NAME);
            DrtRejectionPenaltyProviderConfigGroup penaltyProviderConfigGroup = idfDrtConfigGroup.getDrtRejectionPenaltyProviderConfig();
            DrtRejectionsLinearPenaltyProviderConfigGroup linearPenaltyProviderConfigGroup = null;
            if(penaltyProviderConfigGroup != null) {
                linearPenaltyProviderConfigGroup = (DrtRejectionsLinearPenaltyProviderConfigGroup) penaltyProviderConfigGroup.getPenaltyProviderParams();
            } else {
                linearPenaltyProviderConfigGroup = new DrtRejectionsLinearPenaltyProviderConfigGroup();
                penaltyProviderConfigGroup = new DrtRejectionPenaltyProviderConfigGroup();
                penaltyProviderConfigGroup.addParameterSet(linearPenaltyProviderConfigGroup);
                idfDrtConfigGroup.addParameterSet(penaltyProviderConfigGroup);
            }
            linearPenaltyProviderConfigGroup.setAlpha(this.alphaValues[this.index[0]]);
            linearPenaltyProviderConfigGroup.setInitialRejectionPenalty(this.initialPenaltyValues[this.index[1]]);
            linearPenaltyProviderConfigGroup.setTargetRejectionProbability(this.targetPenaltyValues[this.index[2]]);
            return "_"+linearPenaltyProviderConfigGroup.getAlpha()+"_"+linearPenaltyProviderConfigGroup.getInitialRejectionPenalty()+"_"+linearPenaltyProviderConfigGroup.getTargetRejectionProbability();
        }

        private void increaseIndex() {
            this.index[0]+=1;
            if(this.index[0] == this.alphaValues.length) {
                this.index[0] = 0;
                this.index[1] += 1;
            }
            if(this.index[1] == this.initialPenaltyValues.length) {
                this.index[1] = 0;
                this.index[2] += 1;
            }
        }

        @Override
        public boolean hasNext() {
            if(this.index[2] == this.targetPenaltyValues.length-1) {
                if(this.index[1] == this.initialPenaltyValues.length-1) {
                    return this.index[0] < alphaValues.length-1;
                }
            }
            return true;
        }
    }

    public static void replaceInFile(String filePath, String toReplace, String replacement) throws IOException {
        Path path = Paths.get(filePath);
        Charset charset = StandardCharsets.UTF_8;

        String content = Files.readString(path, charset);
        content = content.replaceAll(toReplace, replacement);
        Files.write(path, content.getBytes(charset));
    }

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("base-config-path", "base-output-dir", "replace-in-file")
                .build();

        ConfigGenerationInterface configGenerator = new DrtRejectionLinearPenaltyProviderSensitivityGenerator(new double[]{0.0}, new double[]{0, 0.05, 0.1}, new double[]{ 0.05, 0.1, 0.3, 0.5, 0.9});

        Path baseConfigPath = Path.of(cmd.getOptionStrict("base-config-path"));
        Path baseDirectoryPath = baseConfigPath.getParent();
        Path baseOutputDirPath = Path.of(cmd.getOptionStrict("base-output-dir"));
        IDFDrtConfigurator configurator = new IDFDrtConfigurator();
        Config config = ConfigUtils.loadConfig(baseConfigPath.toAbsolutePath().toString(), configurator.getConfigGroups());
        String[] replacementArgs = new String[0];
        if(cmd.hasOption("replace-in-file")){
            replacementArgs = cmd.getOptionStrict("replace-in-file").split(",");
        }

        while(configGenerator.hasNext()) {
            String suffix = configGenerator.next(config);
            Path generatedDirPath = baseOutputDirPath.resolve(baseDirectoryPath.getFileName().toString() + suffix);
            FileUtils.copyDirectory(new File(baseDirectoryPath.toAbsolutePath().toString()), new File(generatedDirPath.toAbsolutePath().toString()));
            ConfigUtils.writeConfig(config, generatedDirPath.resolve(baseConfigPath.getFileName()).toAbsolutePath().toString());
            if(replacementArgs.length == 2) {
                replaceInFile(generatedDirPath.resolve(replacementArgs[0]).toAbsolutePath().toString(), replacementArgs[1], replacementArgs[1]+suffix);
            }
        }
    }
}
