# Regions command

## Description

This command displays a list of arena [regions](../regions.md) / their properties.

## Usage

Command |  Definition
------------- | -------------
/pa \<arena\> regions | list all regions of an arena
/pa \<arena\> regions \<region\> | display properties of a given arena region

Example: `/pa free !rs ball` - print properties of the region "ball" of the arena "free"

## Details

The list shows: `<regionname>: <regiontype>, <regionshape>`

The debug prints:

    <arenaname>:<regionname>
    [region type]
    [region shape]
    [region flags]
    [region protections]
    [first position]
    [second position]