#!/usr/bin/perl 
use strict;
use warnings;
use Getopt::Long;

my $iet_volume = "/proc/net/iet/volume";
my $iet_conf = "/etc/iet/ietd.conf";

my $error_code = "";
my $success_code = "";
GetOptions("error_code=s" => \$error_code, "success_code" => \$success_code);

clear_iet_volume() if -e $iet_volume;

clear_iet_conf() if -e $iet_conf;

sub clear_iet_volume {
    open IET_VOLUME, $iet_volume or error($!) and exit 1;
    my @iets = <IET_VOLUME>;
    close IET_VOLUME;
    foreach my $iet(@iets) {
        next unless $iet =~ /tid:\d+/;
        $iet =~ s/.*(tid:\d+).*/$1/g;
        my $tid = (split(/:/, $iet))[1];
        chomp($tid);
        my @result = `ietadm --op delete --tid=$tid 2>&1 > /dev/null`;
        error(toString(@result)) if $? >> 8;
    }
}

sub clear_iet_conf {
    open IET_CONF, $iet_conf or error($!) and exit 1;
    my @configs = <IET_CONF>;
    close IET_CONF;

    open my $fh, "> $iet_conf" or error($!) and exit 1;
    foreach my $config(@configs) {
        next unless $config =~ /\s*#.*/;
        print $fh $config or error($!) and exit 1;
    }
    close $fh;
}

sub error {
    my ($message) = @_;

    my @lines = split(/\n/, $message);
    foreach my $line(@lines) {
        print "[$error_code] $line\n";
    }
}

sub success {
    my ($message) = @_;

    my @lines = split(/\n/, $message);
    foreach my $line(@lines) {
        print "[$success_code] $line\n";
    }
}

sub toString {
    my (@lines) = @_;
    my $string = "";

    foreach my $line(@lines) {
        $string = $string."$line";
    }

    return $string;
}

