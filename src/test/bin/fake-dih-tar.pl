#!/usr/bin/perl -X
# Copyright (C) 2013-2024 Nanjing Pengyun Network Technology Co., Ltd.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 

use strict;
use warnings;
use File::Spec;

my $root_dir = "/tmp/test";
system("rm -rf $root_dir") if -e $root_dir;
system("mkdir -p $root_dir");

my $root_src_dir = File::Spec->catfile($root_dir, "tars");
system("mkdir -p $root_src_dir");
my $root_install_dir = File::Spec->catfile($root_dir, "_packages");
system("mkdir -p $root_install_dir");
my $root_running_dir = File::Spec->catfile($root_dir, "packages");
system("mkdir -p $root_running_dir");


my $dih_dir = "pengyun-instancehub";
my $dih_target_dir = "$dih_dir-2.3.0";
my $dih_target_compressing_file = "$dih_target_dir-internal.tar.gz";
my $dih_config_file = "instancehub.properties";

system("mkdir -p $root_src_dir/$dih_target_dir/config/;");
open CONFIG, "> $root_src_dir/$dih_target_dir/config/$dih_config_file";
print CONFIG "center.dih=localhost:10000\n";
close CONFIG;

chdir($root_src_dir);
system("tar -zcf  $dih_target_compressing_file $dih_target_dir; mv $dih_target_dir $root_install_dir");

chdir("$root_install_dir/$dih_target_dir");
system("mkdir -p $root_running_dir/$dih_dir/config; ln -s $root_install_dir/$dih_target_dir/config/$dih_config_file $root_running_dir/$dih_dir/config/$dih_config_file");
